package snippets;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.client.IWorkItemClient;
import com.ibm.team.workitem.common.model.IAttribute;
import com.ibm.team.workitem.common.model.IWorkItem;

public class query_method {
/*Usage:
 * just call queryValueOfWorkItemByAttributeId()
 * In WorkItemQuery.java
 * you can call this method in findPersonalQuery()
 * 
 * args Info:
 * attributeId: "Category" or "EnvironmentProd" ....
 * workItemClient: was already created in method findPersonalQuery()-WorkItemQuery.java
 * workItem: was already created in method findPersonQuery()-WorkItemQuery.java
 */
	public static String queryValueOfWorkItemByAttributeId(String attributeId, IWorkItemClient workItemClient, IWorkItem workItem) throws IOException {
		
		String result = "";
		try {
			IAttribute attribute;
			attribute = workItemClient.findAttribute(workItem.getProjectArea(), attributeId, null);
			System.out.println("attribute" + workItem.getValue(attribute).toString().split(":")[1]);
			if (null != attribute);
				result = mappingLiteral(workItem.getValue(attribute).toString().split(":")[1]);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	public static String mappingLiteral (String literalString) throws IOException {
		String result = "";
		String encoding = "UTF-8";
		File configure = //new File("C:/Users/IBM_ADMIN/Desktop/resource/attribute.conf");
				//new File(FetchFile.getJarPath() + "/resource/attribute.conf");
				new File(FetchFile.attributeConfigLists);
		if (configure.isFile() && configure.exists()) {
			InputStreamReader reader = new InputStreamReader(new FileInputStream(configure), encoding);
			@SuppressWarnings("resource")
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
	
	public static String resolveEnv(String EnvironmentProd, String Keyword){
		String[] median = EnvironmentProd.split("-");
		return median[1].trim() + median[0].trim().replaceAll(Keyword.split("-")[0], "");
	}
}

