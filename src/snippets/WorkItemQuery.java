package snippets;
/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2007, 2008. All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/


import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.client.TeamPlatform;
import com.ibm.team.repository.client.ITeamRepository.ILoginHandler;
import com.ibm.team.repository.client.ITeamRepository.ILoginHandler.ILoginInfo;
import com.ibm.team.repository.common.IAuditableHandle;
import com.ibm.team.repository.common.IContributor;
import com.ibm.team.repository.common.ItemNotFoundException;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.repository.common.transport.ConnectionException;
import com.ibm.team.process.client.IProcessClientService;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.workitem.client.IQueryClient;
import com.ibm.team.workitem.client.IWorkItemClient;
import com.ibm.team.workitem.common.IAuditableCommon;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.query.IQueryDescriptor;
import com.ibm.team.workitem.common.query.IQueryResult;
import com.ibm.team.workitem.common.query.IResult;
import com.ibm.team.workitem.common.query.QueryTypes;


public class WorkItemQuery {
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

	public static void main(String[] args) {
		System.out.println("xx  " + FetchFile.getJarPath());
		if (args.length < 5) {
			System.out.println("Usage: DupeFinder <repositoryURI> <user> <password> <project area name> <save directory> <Work Item ID>(optional) [<file>]");
			System.exit(1);
		}
		if (args.length > 6){
			System.out.println("Parameter Input Error!");
			System.out.println("Usage: DupeFinder <repositoryURI> <user> <password> <project area name> <save directory> <Work Item ID>(optional) [<file>]");
			System.exit(1);
		}

		InputStream fileInputStream= null;
		
		try {
			TeamPlatform.startup();
			ITeamRepository repository= login(args[0], args[1], args[2]);
			IProgressMonitor monitor = null;
			List<WorkItem> descriptions = new ArrayList<WorkItem>();
			
			// Check if parameters contain work item ID; 5 parameteres: no work item id and polling all work items to find file decriptions; 
			//6 params: there is a work item id and find file descriptions from this work item.
			if(args.length==5)
				descriptions = findPersonalQuery(repository, args[3],"Delivery WorkItem Query",monitor);
			else 
				descriptions = findPersonalQueryByWorkItem(repository, args[3],"Delivery WorkItem Query", args[5],monitor);
			FetchFile.fetchFiles(descriptions, args, repository);
			} catch (ItemNotFoundException e) {
			System.err.println(e.getMessage());
		} catch (ConnectionException e) {
			System.err.println(e.getMessage());
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
		} finally {
			TeamPlatform.shutdown();
			if (fileInputStream != null) {
				try { fileInputStream.close(); } catch (IOException e) {}
			}
		}
	}
	
	/*
	 * find Work Item and its description by work item ID
	 * returns A list that contains one element: String[] = {WorkItemId, Description }
	 * */
	public static List<WorkItem> findPersonalQueryByWorkItem(ITeamRepository teamRepository, String projectAreaName,
			String queryName, String ItemID, IProgressMonitor monitor)
			throws TeamRepositoryException {
		int id = new Integer(ItemID).intValue();
		IWorkItemClient workItemClient = (IWorkItemClient) teamRepository.getClientLibrary(
			IWorkItemClient.class);
		//find work item by its ID
		IWorkItem iWorkItem = workItemClient.findWorkItemById(id, IWorkItem.FULL_PROFILE, monitor);  
		WorkItem workItem = new WorkItem(iWorkItem, workItemClient);
		workItem.setRelatedId(iWorkItem, workItemClient,teamRepository);
		List<WorkItem> query_result = new ArrayList<WorkItem>();
		query_result.add(workItem);
		return query_result;
	}	
	
	/*
	 * find Work Item with type "delivery" and state "new"
	 * returns A list that contains all workitems' id and description: String[] = {WorkItemId, Description }
	 * */
	public static List<WorkItem> findPersonalQuery(ITeamRepository teamRepository, String projectAreaName,
			String queryName, IProgressMonitor monitor)
			throws TeamRepositoryException {
		IProcessClientService processClient= (IProcessClientService) teamRepository.getClientLibrary(IProcessClientService.class);
		URI uri= URI.create(projectAreaName.replaceAll(" ", "%20"));
		IProjectArea projectArea= (IProjectArea) processClient.findProcessArea(uri, null, null);
		IWorkItemClient workItemClient = (IWorkItemClient) teamRepository.getClientLibrary(
			IWorkItemClient.class);
		IQueryClient queryClient = workItemClient.getQueryClient();
		// Get the current user.
		IContributor loggedIn = teamRepository.loggedInContributor();
		IQueryDescriptor queryToRun = null;
		// Get all queries of the user in this project area.
		List<?> queries = queryClient.findPersonalQueries(
			projectArea.getProjectArea(), loggedIn,
			QueryTypes.WORK_ITEM_QUERY,
			IQueryDescriptor.FULL_PROFILE, monitor);
		// Find a query with a matching name
		for (Iterator<?> iterator = queries.iterator(); iterator.hasNext();) {
			IQueryDescriptor iQueryDescriptor = (IQueryDescriptor) iterator.next();
			if (iQueryDescriptor.getName().equals(queryName)) {
				queryToRun = iQueryDescriptor;
				break;
			}
		}
		System.out.println(queryClient.countQueryResults(queryToRun, monitor)+"XXX");
		IQueryResult<?> results = queryClient.getQueryResults(queryToRun);
		System.out.println(results);
		List<WorkItem> query_result = new ArrayList<WorkItem>();

		while (results.hasNext(monitor)) {
			IResult result = (IResult) results.next(monitor);
			IAuditableCommon auditableCommon = (IAuditableCommon) teamRepository.getClientLibrary(IAuditableCommon.class);
			IWorkItem iWorkItem = auditableCommon.resolveAuditable(
				(IAuditableHandle) result.getItem(), IWorkItem.FULL_PROFILE, monitor);
			WorkItem workItem = new WorkItem(iWorkItem, workItemClient);
			workItem.setRelatedId(iWorkItem, workItemClient, teamRepository);

			String workItem_description = workItem.getDescription();
			if (workItem_description != null && workItem_description.length() > 0) {
				System.out.print("WorkItem Id: " + workItem.getId() + "; ");
				query_result.add(workItem);
			}
		}		
		return query_result;
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
}
