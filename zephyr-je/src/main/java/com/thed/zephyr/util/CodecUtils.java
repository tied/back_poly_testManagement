package com.thed.zephyr.util;
import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;


public class CodecUtils {
    private static final Logger log = Logger.getLogger(CodecUtils.class);
	private static final int KEY_LENGTH = 8;
	private static String keyString = ApplicationConstants.ZFJ_PLUGIN_KEY;
	
	/**
	 * encrypts the data using customerId as default key. Uses axis base64 utility (because its SOAP safe)
	 * @param data
	 * @return
	 */
	public String encrypt(String data) {
		try{
			return encrypt(data, generateKey(keyString, null));
		} catch (Exception e) {
			log.error("Crypto.encrypt() Exception: ", e);
			return null;
		}
	}
	
	private String encrypt(String plain, Key key) throws Exception{
		Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, key);

		byte[] inputBytes = plain.getBytes("UTF8");
		byte[] outputBytes = cipher.doFinal(inputBytes);

		//String base64 = new String(org.apache.axis.encoding.Base64.encode(outputBytes));
		String base64 = new String(Base64.encodeBase64(outputBytes));
		
		return base64;
	}
	
	/**
	 * decrypts the data using customerId as default key
	 * @param plain
	 * @return
	 */
	public String decrypt(String plain) {
		try{
			return decrypt(plain, generateKey(keyString, null));
		} catch (Exception e) {
			log.error("Crypto.encrypt() Exception: ", e);
			return null;
		}
	}

	private String decrypt(String raw,  Key key) throws Exception {
		Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
		cipher.init(Cipher.DECRYPT_MODE, key);
		
		//byte[] inputBytes = org.apache.axis.encoding.Base64.decode(raw);
		byte[] inputBytes = Base64.decodeBase64(raw.getBytes());
		byte[] outputBytes = cipher.doFinal(inputBytes);

		String result = new String(outputBytes, "UTF8");

		return result;
	}
	
	/**
	 * Uses simple SecretKeySpec (KeyGenerator is not used as it adds randomness to the key and decrypt fails
	 * @param keyString Length should be equal to or less then 16 
	 * @param algorithm Defaults to DES
	 * @return generated Key
	 * @throws Exception
	 * @see {@link SecretKeySpec}
	 */
	private Key generateKey(String keyString, String algorithm) throws Exception{
		if(StringUtils.isBlank(algorithm)){
			algorithm = "DES";
		}
		keyString = StringUtils.rightPad(keyString, KEY_LENGTH, '-');
		
		DESKeySpec spec = new DESKeySpec(keyString.getBytes());
		SecretKeyFactory factory = SecretKeyFactory.getInstance(algorithm);
		Key key = factory.generateSecret(spec);
		
		return key;
		
	}
}
