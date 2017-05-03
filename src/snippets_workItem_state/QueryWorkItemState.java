package snippets_workItem_state;

/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2007, 2008. All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.team.foundation.common.text.XMLString;
import com.ibm.team.links.common.IItemReference;
import com.ibm.team.links.common.IReference;
import com.ibm.team.repository.client.IItemManager;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.client.TeamPlatform;
import com.ibm.team.repository.client.ITeamRepository.ILoginHandler;
import com.ibm.team.repository.client.ITeamRepository.ILoginHandler.ILoginInfo;
import com.ibm.team.repository.common.IItem;
import com.ibm.team.repository.common.IItemHandle;
import com.ibm.team.repository.common.ItemNotFoundException;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.repository.common.transport.ConnectionException;
import com.ibm.team.workitem.client.IWorkItemClient;
import com.ibm.team.workitem.common.model.IAttribute;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemReferences;
import com.ibm.team.workitem.common.model.WorkItemEndPoints;

public class QueryWorkItemState {
	
private static class LoginHandler implements ILoginHandler, ILoginInfo {
		
		private String fUserId;
		private String fPassword;
		
		private LoginHandler(String userId, String password) {
			fUserId= userId;
			fPassword= password;
		}
		
		public String getUserId() {
			return fUserId;
		}
		
		public String getPassword() {
			return fPassword;
		}
		
		public ILoginInfo challenge(ITeamRepository repository) {
			return this;
		}
	}
	/*
	 * MAIN
	 * Args:
	 * args[0]: respositoryURI
	 * args[1]: username
	 * args[2]: password
	 * args[3]: project area name
	 * args[4]: workItem Type
	 * args[5]: workItem ID
	 */
	public static void main(String[] args) throws IOException {

		if (args.length != 6) {
			System.out.println("Usage: DupeFinder <repositoryURI> <user> <password> <project area name> <WorkItem Type> <WorkItem ID>");
			System.exit(1);
		}
		try {
			TeamPlatform.startup();
			ITeamRepository repository= login(args[0], args[1], args[2]);
			IProgressMonitor monitor = null;
			System.out.println(queryWorkItemStateById(repository, args[3], args[4], args[5], monitor));
			} catch (ItemNotFoundException e) {
			System.err.println(e.getMessage());
		} catch (ConnectionException e) {
			System.err.println(e.getMessage());
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
		} finally {
			TeamPlatform.shutdown();
		}
	}
	
	/*
	 * login
	 * */
	static ITeamRepository login(String repositoryURI, String userId, String password) throws TeamRepositoryException {

		ITeamRepository teamRepository= TeamPlatform.getTeamRepositoryService().getTeamRepository(repositoryURI);
		teamRepository.registerLoginHandler(new LoginHandler(userId, password));
		teamRepository.login(null);
		return teamRepository;
	}
	
	/*
	 * get parent workItem id
	 */
	public static String queryParentWorkItemId(ITeamRepository teamRepository, IWorkItemClient workItemClient, IWorkItem workItem) throws TeamRepositoryException {
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
	/*
	 * find Work Item by work item ID
	 * */
	public static String queryWorkItemStateById(ITeamRepository teamRepository, String projectAreaName,
			String queryName, String ItemID, IProgressMonitor monitor)
			throws TeamRepositoryException, IOException {

		int id = new Integer(ItemID).intValue();
		IWorkItemClient workItemClient = (IWorkItemClient) teamRepository.getClientLibrary(
			IWorkItemClient.class);
		//find work item by its ID
		IWorkItem iWorkItem = workItemClient.findWorkItemById(id, IWorkItem.FULL_PROFILE, monitor);  
		String literalString = iWorkItem.getState2().toString().split(":")[1];
		
		System.out.println(literalString);
		String filePath = QueryWorkItemState.class.getProtectionDomain().getCodeSource().getLocation().getFile();
		if (filePath.endsWith(".jar")) {

			filePath = filePath.substring(0, filePath.lastIndexOf("/"));  
	    }
		IAttribute attribute = null;
	
		attribute = workItemClient.findAttribute(iWorkItem.getProjectArea(), "com.ibm.team.workitem.attribute.summary", null);
		System.out.println("{}{}{}" + iWorkItem.getHTMLSummary());
		
		((IWorkItem)iWorkItem.getWorkingCopy()).getComments().getContents();
		
		//iWorkItem.setHTMLSummary(XMLString.createFromPlainText("Test"));
		
		String tmp = queryParentWorkItemId(teamRepository, workItemClient, iWorkItem);
		
		XMLString description = null;
		description = iWorkItem.getHTMLDescription();
		description = description.concat(description);
		
		System.out.println("~~~~~~" + iWorkItem.getTags() + "``````" + iWorkItem.getHTMLDescription());

		
		
		FileWriter parentWorkItemId = null;
		parentWorkItemId = new FileWriter(filePath + "/parent_workItem_id");
		parentWorkItemId.write(tmp);
		parentWorkItemId.close();
		
		String result = "";
		String encoding = "UTF-8";
		File configure = new File(filePath + "/resource/workItem_states_map.conf");
		if (configure.isFile() && configure.exists()) {
			InputStreamReader reader = new InputStreamReader(new FileInputStream(configure), encoding);
			BufferedReader bufferedReader = new BufferedReader(reader);
			String lineText = null;
			while ((lineText = bufferedReader.readLine()) != null) {
				if (lineText.contains(literalString))
					result = lineText.split(":")[1];
					result = result.trim();
			}
		} else {
			System.out.println("Cannot find the Configure file");
		}
		return result;
	}	
}
