package snippets;

import java.io.IOException;
import java.util.List;

import com.ibm.team.links.common.IItemReference;
import com.ibm.team.links.common.IReference;
import com.ibm.team.repository.client.IItemManager;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.common.IItem;
import com.ibm.team.repository.common.IItemHandle;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.client.IWorkItemClient;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemReferences;
import com.ibm.team.workitem.common.model.WorkItemEndPoints;

public class WorkItem{
	private String Id;
	private String Description;
	private IWorkItem workItem;
	private IWorkItemClient workItemClient;
	private String Category;
	private String EnvironmentProd;
	private String Param;
	private String Keyword;
	private String State;
	private String ParentId;
	private String RelatedId;
	public WorkItem(IWorkItem wi, IWorkItemClient wiClient){
		workItem = wi;
		workItemClient = wiClient;
		params();
		State = wi.getWorkItemType();
	}
	private void params(){
		Id = Integer.toString(workItem.getId());
		Description = workItem.getHTMLDescription().toString();
	}
	public IWorkItem getWorkItem(){
		return workItem;
	}
	public void setParentId(IWorkItem wi, IWorkItemClient wiClient, ITeamRepository teamRepository){
		try {
			ParentId = queryParentWorkItemId(teamRepository, wiClient, wi);
		} catch (TeamRepositoryException e) {
			System.out.println("ParentId required failed");
			e.printStackTrace();
		}
	}
	public void setRelatedId(IWorkItem wi, IWorkItemClient wiClient, ITeamRepository teamRepository){
		try {
			RelatedId = queryRelatedWorkItemId(teamRepository, wiClient, wi);
		} catch (TeamRepositoryException e) {
			System.out.println("RelatedId required failed");
			e.printStackTrace();
		}
	}
	public String getState(){
		return this.State;
	}
	public IWorkItemClient getWorkItemClient(){
		return workItemClient;
	}
	public String getId(){
		return this.Id;
	}
	public String getCategory(){
		return this.Category;
	}
	public String getEnvironmentProd(){
		return this.EnvironmentProd;
	}
	public String getKeyword(){
		return this.Keyword;
	}
	public String getParam(){
		return this.Param;
	}
	public String getRelatedId(){
		return this.RelatedId;
	}
	public String getParentId(){
		return this.ParentId;
	}
	public void setAttribute(){
		try {
			Category = query_method.queryValueOfWorkItemByAttributeId("Categories", workItemClient, workItem);
			EnvironmentProd = query_method.queryValueOfWorkItemByAttributeId("environment", workItemClient, workItem);
			Keyword = query_method.mappingLiteral(Category);
			Param = query_method.resolveEnv(EnvironmentProd, Keyword);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public String getDescription(){
		return this.Description;
	}
	public String getAttribute(String attributeId){
		String attribute = "";
		try {
			attribute = query_method.queryValueOfWorkItemByAttributeId(attributeId, workItemClient, workItem);
		} catch (IOException e) {
			System.out.println("Attribute id input error");
			e.printStackTrace();
		}
		return attribute;
	}
	/*
	 * get parent workItem id
	 */
	public String queryParentWorkItemId(ITeamRepository teamRepository, IWorkItemClient workItemClient, IWorkItem workItem) throws TeamRepositoryException {
		IWorkItemReferences refs;
		String result = null;
		refs = workItemClient.resolveWorkItemReferences(workItem, null);
		List <IReference> references = refs.getReferences(WorkItemEndPoints.PARENT_WORK_ITEM);
		for (IReference ref: references) {
			if(ref.isItemReference()) {
        	    IItemReference refItem = (IItemReference) ref;
        	    IItemHandle itemHandle = refItem.getReferencedItem();
        	    IItem item = teamRepository.itemManager().fetchCompleteItem(itemHandle, IItemManager.DEFAULT, null);
        	    if(item instanceof IWorkItem) {
        	    	System.out.println("parent item : " +((IWorkItem)item).getId());
        	    	result = ((IWorkItem)item).getId() + "";
        	    }
        	 }
		}
		return result;
	}
	public String queryRelatedWorkItemId(ITeamRepository teamRepository, IWorkItemClient workItemClient, IWorkItem workItem) throws TeamRepositoryException {
		IWorkItemReferences refs;
		String result = null;
		refs = workItemClient.resolveWorkItemReferences(workItem, null);
		List <IReference> references = refs.getReferences(WorkItemEndPoints.RELATED_WORK_ITEM);
		for (IReference ref: references) {
			if(ref.isItemReference()) {
        	    IItemReference refItem = (IItemReference) ref;
        	    IItemHandle itemHandle = refItem.getReferencedItem();
        	    IItem item = teamRepository.itemManager().fetchCompleteItem(itemHandle, IItemManager.DEFAULT, null);
        	    if(item instanceof IWorkItem) {
        	    	System.out.println("parent item : " +((IWorkItem)item).getId());
        	    	result = ((IWorkItem)item).getId() + "";
        	    }
        	 }
		}
		return result;
	}
}