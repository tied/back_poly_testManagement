package com.thed.zephyr.util;

import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.sal.api.ApplicationProperties;
import com.thed.zephyr.je.attachment.AttachmentUtils;
import com.thed.zephyr.je.operation.JobProgress;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Masud on 6/20/17.
 */
public class ZephyrUtil {

    private static final Logger log = LoggerFactory.getLogger(ZephyrUtil.class);
    private static final String ERROR_LOG_MESSAGE = "[Error] [Error code: %s %s Error Message : %s]";
    /**
     * Convert object to String
     * @param value
     * @return
     */
    public static String getString(Object value){

        try {
            return  new ObjectMapper().writeValueAsString(value);
        } catch (IOException e) {
            e.printStackTrace();
            log.error("Error during convert to string");
            return null;
        }
    }

    /**
     * Get JobProgress from String
     * @param jobProgressStr
     * @return
     */
    public static JobProgress getJobProgressFromStr(String jobProgressStr){
        JobProgress jobProgress = null;
        try {
            jobProgress = new ObjectMapper().readValue(jobProgressStr, JobProgress.class);
        } catch (IOException e) {
            e.printStackTrace();
            log.error("Error during convert job progress from string");
        }
        return jobProgress;
    }

    public static JSONObject validateUser(ApplicationUser user, JiraAuthenticationContext authContext) {
        JSONObject jsonObject = new JSONObject();
        try {
            if(user == null && !JiraUtil.hasAnonymousPermission(user)) {
                jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
                return jsonObject;
            }
        } catch (JSONException e) {
            log.error("Error occurred while getting count.",e);
            return null;
        }
        return null;
    }

    public static Response constructErrorResponse(JSONObject jsonObject, String errorMessage, Response.Status status, Exception exception) {
        try {
            String finalErrorMessage = String.format(ERROR_LOG_MESSAGE, status.getStatusCode(), status, errorMessage);
            log.error(finalErrorMessage, exception);
            jsonObject.put("error", errorMessage);
            return Response.status(status).entity(jsonObject != null ? jsonObject.toString() : finalErrorMessage).cacheControl(ZephyrCacheControl.never()).build();
        } catch (JSONException e) {
            log.error("Error while constructing the error response");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    public static Response constructErrorResponse(JSONObject jsonObject, Response.Status status, Exception exception) {
        try {
            String finalErrorMessage = String.format(ERROR_LOG_MESSAGE, status.getStatusCode(), status, jsonObject.get("error"));
            log.error(finalErrorMessage, exception);
            return Response.status(status).entity(jsonObject != null ? jsonObject.toString() : finalErrorMessage).cacheControl(ZephyrCacheControl.never()).build();
        } catch (JSONException e) {
            log.error("Error while constructing the error response");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
    /**
     * Builds an Export file
     * @param file
     * @return
     */
    public static Response buildExportResponse(File file) {
		if (file != null) {
			JSONObject ob = new JSONObject();
			try {
				ApplicationProperties applicationProperties = (ApplicationProperties) ZephyrComponentAccessor
						.getInstance().getComponent("applicationProperties");
				String fileUrl = applicationProperties.getBaseUrl()
						+ "/plugins/servlet/export/exportAttachment?fileName=" + file.getName();
				ob.put("url", fileUrl);
				return Response.ok(ob.toString()).build();
			} catch (JSONException e) {
				log.warn("Error exporting file", e);
				return Response.status(Status.SERVICE_UNAVAILABLE).build();
			}
		} else {
			log.error("[Error] [Error code:" + Status.SERVICE_UNAVAILABLE.getStatusCode() + " "
					+ Status.SERVICE_UNAVAILABLE + " Error Message : Service unavailable to export.");
			return Response.status(Status.SERVICE_UNAVAILABLE).build();
		}
	}

    /**
     * Creates a attachment
     * @param so
     * @param fileName
     * @return
     */
    public static File createAttachment(final StreamingOutput so, String fileName) {
		final File tmpDir = AttachmentUtils.getTemporaryAttachmentDirectory();
		File tempAttachmentFile = new File(tmpDir, fileName);
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(tempAttachmentFile);
			so.write(out);
		} catch (FileNotFoundException e) {
			log.error("createAttachment() : FileNotFoundException", e);
		} catch (WebApplicationException e) {
			log.error("createAttachment() : WebApplicationException", e);
		} catch (IOException e) {
			log.error("createAttachment() : IOException", e);
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					log.error("createAttachment() : IOException closing stream", e);
					return null;
				}
			}
		}
		return tempAttachmentFile;
	}
}
