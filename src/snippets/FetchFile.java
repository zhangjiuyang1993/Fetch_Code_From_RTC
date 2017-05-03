package snippets;
/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2007, 2008. All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.ibm.team.foundation.common.text.XMLString;
import com.ibm.team.repository.client.IContributorManager;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.common.IContributor;
import com.ibm.team.workitem.client.IDetailedStatus;
import com.ibm.team.workitem.client.IWorkItemClient;
import com.ibm.team.workitem.client.IWorkItemWorkingCopyManager;
import com.ibm.team.workitem.client.WorkItemWorkingCopy;
import com.ibm.team.workitem.common.model.IComment;
import com.ibm.team.workitem.common.model.IComments;
import com.ibm.team.workitem.common.model.IWorkItem;

public class FetchFile {
	
	public static String filelists = getJarPath() + "resource" + File.separator + "sfilelist.txt";
	public static String batPath =  getJarPath() + "resource" + File.separator;
	public static String attributeLists = getJarPath() + "resource" + File.separator + "attribute.txt";
	public static String attributeConfigLists = getJarPath() + "resource" + File.separator + "attribute.conf";
	public static String parameterFile = getJarPath() + "resource" + File.separator + "param.txt";
	public static String UCD_Component = "UCD_Component";
	public static String UCD_Env = "UCD_Environment";
	public static String UCD_Proc = "UCD_Process";
	public static String UCD_Res = "UCD_Resource";
	public static String loadFiles = getJarPath() + "resource" + File.separator +"loadFiles.txt";
	
	/*
	 * input a list with work items' id and description
	 * resolve the description to get the stream, component and files names
	 * loadfiles and fetch files to local
	 * */
	public static void fetchFiles(List<WorkItem> descriptions, String[] info
			, ITeamRepository teamRepository){
		IWorkItemClient workItemClient = (IWorkItemClient) teamRepository
				.getClientLibrary(IWorkItemClient.class);
		try {
			FileOutputStream out = new FileOutputStream(new File(attributeLists));
			if(descriptions.isEmpty()) 
				System.out.println("No files need fetch");
			else{
	        	for(WorkItem temp: descriptions ){
	        		
					System.out.println("filelists - " + filelists);
					System.out.println("attributeLists - " + attributeLists);
	        		
	        		String dsc = temp.getDescription();
	        		String id = temp.getId();
	        		if(!(dsc.contains("Stream")&&dsc.contains("Component")&&dsc.contains("Path"))) continue;
	        		temp.setAttribute();
	        		String line = "WorkItem Id: " + id + "; " 
	        				+"Related Id: " + temp.getRelatedId() + "; "
	        				+"Category: " + temp.getCategory() + "; "
	        				+"EnvironmentProd: " + temp.getEnvironmentProd();
	        		System.out.print("WorkItem Id: " + temp.getId() + "; ");
					System.out.println("Category: " + temp.getCategory() + "; ");
					System.out.println("EnvironmentProd: " + temp.getEnvironmentProd() );
	        		System.out.println(dsc);
	        		
	        		String dscClean = dsc.replaceAll("\\s+", "").replaceAll("Stream:", " ")
	        	.replaceAll("Component:", " ").replaceAll("Path:"," ").replaceAll("<br/>"," ").replaceAll("<p>", " ").replaceAll("<.*?>","").replaceAll("\\s+", " ").trim();
	        		String[] list = dscClean.split(" ");
	        		if(list.length < 3) continue;
	        		String stream = list[0];
	        		String component = list[1];
	        		String[] files = new String[list.length-2];
	        		for(int i = 0; i < list.length-2; i++){
	        			files[i] = list[i+2];
	        		}
	            	if(!(stream == null && component == null && files.length == 0))
	            		out.write((line+"\r\n").getBytes()); 
	            	String version = loadFiles(temp.getRelatedId(), stream, component, files, info);
	            	writeParams(temp, stream, component);
	            	boolean x = addComment(teamRepository, workItemClient, new Integer(id).intValue(),
	            			version, info[1]);
	            	System.out.println("add comment result: " + x);
	        	}
	            System.out.println("Load Files Done. ");
	        }
			out.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}   
	}
	
	public static String getJarPath(){
		
		String filePath = WorkItemQuery.class.getResource("/").getPath();
		filePath = filePath.replace("/", File.separator).replace("\\\\",File.separator);
		if (!File.separator.equals("/"))
			filePath = filePath.substring(1);
		if (filePath.endsWith(".jar")) {
			filePath = filePath.substring(0, filePath.lastIndexOf(File.separator));  
	    }
		return filePath;
	}
	
	/*
	 *load files with stream, component and files names;
	 *use local batch files to fetch files; using scm tools. 
	 * */
    private static String loadFiles(String RelatedId, String stream, String component, 
    		String[] files, String[] info){
		FileOutputStream out = null;
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
    	String date = df.format(new Date());
    	String folderName = RelatedId+"_"+date;
    	try {
    		File directory = new File(info[4] + folderName);
    		
            out = new FileOutputStream(new File(filelists));   
            for (String file: files) {   
            	String line = file.trim();
            	if(line.isEmpty()) continue;
                out.write(("/" + line+"\r\n").replaceAll("//", "/").getBytes());
            }
            out.close();
 
            //String Command = batPath + "start.bat" + " " + info[0] + " " + info[1] + " " + info[2];
            String Command = batPath + "extract.bat" + " " + filelists + " " + parameterFile + " " + directory; 
            
            System.out.println("cli: ");
            System.out.println(Command);
            //runcmd(Command);
            runcmd(Command);
        }catch (Exception e) {
             e.printStackTrace();   
        }
    	return folderName;
    }
    
    /**
     * Deal with Runtime.getRuntime().exec Errors
     */ 
    private static void runcmd(String command){
    	Runtime r=Runtime.getRuntime(); 
        Process p=null; 
        try{  
        		p = r.exec(command); 
        		StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(), "ERROR");          
        		errorGobbler.start(); 
        		StreamGobbler outGobbler = new StreamGobbler(p.getInputStream(), "STDOUT"); 
        		outGobbler.start(); 
        		p.waitFor(); 
        }catch(Exception e){  
            System.out.println("Run Error:"+e.getMessage()); 
            e.printStackTrace();  
        }  
	}
    
    
	private static boolean addComment(ITeamRepository teamRepository, IWorkItemClient workItemClient, 
			int workItemId, String version, String byUser) throws Exception{
		System.out.println(byUser + " " + version + " " +workItemId);
		IWorkItem workItem = workItemClient.findWorkItemById(workItemId, IWorkItem.SMALL_PROFILE, null);
		IWorkItemWorkingCopyManager wcm = workItemClient.getWorkItemWorkingCopyManager();
		wcm.connect(workItem, IWorkItem.FULL_PROFILE, null);
		XMLString comment = XMLString.createFromPlainText(version);
		try {
				WorkItemWorkingCopy wc = wcm.getWorkingCopy(workItem);
				IComments comments = wc.getWorkItem().getComments();
				IContributorManager contributorManager = teamRepository.contributorManager();
				IContributor contributor = contributorManager.fetchContributorByUserId(byUser, null);
				IComment newComment = comments.createComment(contributor, comment);
				comments.append(newComment);
				IDetailedStatus s = wc.save(null);
				comment.toString();
				if (!s.isOK()) {
					throw new Exception(byUser, s.getException());
				}
				System.out.println("Comment " + comment.toString() + " added. ");
		} finally {
			wcm.disconnect(workItem);
		}
		return true;
	}
	
	/*
	 * write UCD's Parameters to the File
	 */
    private static void writeParams(WorkItem wi, String stream, String component){
    	FileOutputStream out = null;
    	String param = wi.getParam();
    	String keyword = wi.getKeyword();
    	String keyValue = keyword.split("_")[0].trim();
    	String propertyName = keyword.split("_")[1].trim();
    	try {
    		String env = UCD_Env + ": " + query_method.mappingLiteral(UCD_Env).replaceAll("\\*", keyValue);
        	String ucd_component = UCD_Component + ": " + query_method.mappingLiteral(UCD_Component).replaceAll("\\*", keyValue);
        	String proc = UCD_Proc + ": " + query_method.mappingLiteral(UCD_Proc).replaceAll("\\*", keyValue);
        	String res = UCD_Res + ": " + query_method.mappingLiteral(UCD_Res).replaceAll("\\*", keyValue);
        	String property = "Resource_" + propertyName + ": " + param ;
        	String wiId = "WorkItem_Id: " + wi.getId(); 
        	String strm = "Stream: " + stream;
        	String comp = "Component: " + component;
        	System.out.println("Stream: " + strm + "; Component: " +comp);
            out = new FileOutputStream(new File(parameterFile));  
            out.write((env+"\r\n").getBytes());
            out.write((ucd_component+"\r\n").getBytes());
            out.write((proc+"\r\n").getBytes());
            out.write((res+"\r\n").getBytes());
            out.write((property+"\r\n").getBytes());
            out.write((wiId+"\r\n").getBytes());
            out.write((strm+"\r\n").getBytes());
            out.write((comp+"\r\n").getBytes());
            System.out.println("params loaded");
            out.close();
        }catch (Exception e) {
             e.printStackTrace();   
        }
    }
}
