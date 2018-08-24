package com.thed.zephyr.je.transformer;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dom4j.Element;

import com.atlassian.plugin.elements.ResourceLocation;
import com.atlassian.plugin.servlet.DownloadableResource;
import com.atlassian.plugin.webresource.transformer.CharSequenceDownloadableResource;
import com.atlassian.plugin.webresource.transformer.WebResourceTransformer;
import com.atlassian.security.random.DefaultSecureTokenGenerator;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.CodecUtils;

public class ZephyrKeyTransformer implements WebResourceTransformer {
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("@([a-zA-Z][a-zA-Z0-9_]*)");

	@Override
	public DownloadableResource transform(Element configElement,
			ResourceLocation location, String filePath,
			DownloadableResource downloadableResource) {
        return new ZephyrKeyDownloadableResource(downloadableResource);
	}
	
	
    static class ZephyrKeyDownloadableResource extends CharSequenceDownloadableResource {
		private final Map<String, String> params = new HashMap<String, String>();
		
		public ZephyrKeyDownloadableResource(DownloadableResource originalResource)
		{
		    super(originalResource);
	        final String crytoPart = DefaultSecureTokenGenerator.getInstance().generateToken();
	        final String cryptedString = System.currentTimeMillis() + "|" + ApplicationConstants.ACCESS_ALL + "|" + crytoPart;
	        CodecUtils codecUtils = new CodecUtils();
			String encryptedString = codecUtils.encrypt(cryptedString);
			String encKeyFld = System.getProperty("zephyr.header.field", ApplicationConstants.ENCRYPTED_STRING);
			params.put("encKeyFld", encKeyFld);
	        params.put("encKeyVal", encryptedString);
		}

       @Override
        protected CharSequence transform(CharSequence original)
        {
            final Matcher matcher = VARIABLE_PATTERN.matcher(original);
            int start = 0;
            StringBuilder out = null;
            while (matcher.find())
            {
                if (out == null)
                {
                    out = new StringBuilder();
                }

                out.append(original.subSequence(start, matcher.start()));
                String token = matcher.group(1);
                String subst = params.get(token);
                if (subst != null)
                {
                    out.append(subst);
                }
                else
                {
                    out.append(matcher.group());
                }
                start = matcher.end();
            }
            if (out == null)
            {
                return original;
            }
            else
            {
                out.append(original.subSequence(start, original.length()));
                return out.toString();
            }
        }
    }
}
