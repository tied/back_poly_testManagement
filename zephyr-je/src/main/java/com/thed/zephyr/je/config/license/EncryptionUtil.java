/*
 * $Id$
 *
 *  Copyright (c) 2003 SourceTap - www.sourcetap.com
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.thed.zephyr.je.config.license;

import java.io.*;
import java.security.*;
import java.security.spec.*;

/**
 * Provides utility functions to handle public/private key encryption.
 *
 * @author Steve Fowler
 * @version $Revision$
 */
public class EncryptionUtil
{
	private PublicKey publicKey = null;
	private PrivateKey privateKey = null;
	
	public EncryptionUtil()
	{
	}

	/**
	* Generate a public/private key pair 
	*/
	public void generateKeys() 
		throws IOException, NoSuchAlgorithmException, NoSuchProviderException
	{
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA");
		keyGen.initialize(1024, new SecureRandom());
		KeyPair pair = keyGen.generateKeyPair();
		this.privateKey = pair.getPrivate();
		this.publicKey = pair.getPublic();
	}

	/**
	* Generate a public/private key pair, and write the keys to the specified files
	* @param publicURI   name of file to store the public key in
	* @param privateURI  name of file to store the private key in
	*/
	public void generateKeys( String publicURI, String privateURI )
		throws IOException, NoSuchAlgorithmException, NoSuchProviderException
	{
		generateKeys();
		writeKeys(publicURI, privateURI);
	}

	public PublicKey getPublic()
	{
		return publicKey;
	}
	
	public PrivateKey getPrivate()
	{
		return privateKey;
	}
	
	public void writeKeys(String publicURI, String privateURI) throws IOException, FileNotFoundException
	{
		writePublicKey(publicURI);
		writePrivateKey(privateURI);
	}
	
	public void writePublicKey(String URI) throws IOException, FileNotFoundException
	{
		byte[] enckey = publicKey.getEncoded();
		FileOutputStream keyfos = new FileOutputStream(URI);
		keyfos.write(enckey);
		keyfos.close();
	}
	
	public void writePrivateKey(String URI) throws IOException, FileNotFoundException
	{
		byte[] enckey = privateKey.getEncoded();
		FileOutputStream keyfos = new FileOutputStream(URI);
		keyfos.write(enckey);
		keyfos.close();
	}
	
	/**
	* read public/private keys from specified files
	* @param publicURI   name of public key file
	* @param privateURI  name of private key file
	*/
	public void readKeys(String publicKeyString, String privateKeyString)
		throws IOException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException
	{
		readPublicKey(publicKeyString);
		readPrivateKey(privateKeyString);
	}

	/**
	* read public key from specified file
	* @param publicURI   name of public key file
	* @return PublicKey  public key
	*/
	public PublicKey readPublicKey(String URI) 
		throws IOException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException
	{
		FileInputStream keyfis = new FileInputStream(URI);
		byte[] encKey = new byte[keyfis.available()];
		keyfis.read(encKey);
		keyfis.close();
		X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(encKey);
		KeyFactory keyFactory = KeyFactory.getInstance("DSA");
		publicKey = keyFactory.generatePublic(pubKeySpec);
		return publicKey;
	}

	/**
	* read public key from byte array
	* @param publicURI   name of public byte array
	* @return PublicKey  public key
	*/
	public PublicKey readPublicKeyFromBytes(byte[] publicKeyByteArry) 
			throws IOException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException
		{
			X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(publicKeyByteArry);
			KeyFactory keyFactory = KeyFactory.getInstance("DSA");
			publicKey = keyFactory.generatePublic(pubKeySpec);
			return publicKey;
		}

	
	/**
	* read private key from specified file
	* @param privateURI   name of private key file
	* @return PrivateKey  private key
	*/
	public PrivateKey readPrivateKey(String URI) 
		throws IOException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException
	{
		FileInputStream keyfis = new FileInputStream(URI);
		byte[] encKey = new byte[keyfis.available()];
		//System.out.println(encKey);

		keyfis.read(encKey);
		keyfis.close();
		PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(encKey);
		KeyFactory keyFactory = KeyFactory.getInstance("DSA");
		privateKey = keyFactory.generatePrivate(privKeySpec);
		////System.out.println("Private Key From File - " + privateKey);
		return privateKey;
	}
	
	/**
	* read private key from specified byte Array
	* @param privateURI   name of private key byte array
	* @return PrivateKey  private key
	*/
	public PrivateKey readPrivateKeyFromBytes(byte[] privateKeyByteArry) 
			throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException
		{
			PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(privateKeyByteArry);
			KeyFactory keyFactory = KeyFactory.getInstance("DSA");
			privateKey = keyFactory.generatePrivate(privKeySpec);
			return privateKey;
		}
	
	/**
	* sign a message using the private key
	* @param message   the message to be signed
	* @return String  the signed message encoded in Base64
	*/
	public String sign(String message) 
		throws IOException, NoSuchAlgorithmException, NoSuchProviderException,
		InvalidKeySpecException, InvalidKeyException, SignatureException
	{
		return sign(message, privateKey);
	}

	/**
	* sign a message using the private key
	* @param message   the message to be signed
	* @param message   the name of the file containing the private key
	* @return String  the signed message encoded in Base64
	*/
	public String sign(String message, String privateKeyURI) 
		throws IOException, NoSuchAlgorithmException, NoSuchProviderException,
		InvalidKeySpecException, InvalidKeyException, SignatureException, IOException
	{
		PrivateKey pk = readPrivateKey(privateKeyURI);
		return sign(message, pk);
	}

	/**
	* sign a message using the private key
	* @param message   the message to be signed
	* @param message   the private key
	* @return String   the signed message encoded in Base64
	*/
	public String sign(String message, PrivateKey privateKey) 
		throws IOException, NoSuchAlgorithmException, NoSuchProviderException,
		InvalidKeySpecException, InvalidKeyException, SignatureException
	{
		Signature dsa = Signature.getInstance("SHA/DSA");
		dsa.initSign(privateKey);
		dsa.update(message.getBytes());
		byte m1[] = dsa.sign();
		
		String signature = new String(Base64Coder.encode(m1));
		
		return signature;
	}
	
	/**
	* verify that the message was signed by the private key by using the public key
	* @param message     the message to be verified
	* @param signature   the signature generated by the private key and encoded in Base64
	* @param publicKeyURI   the name of the file containing the public key
	* @return boolean   true if the message was signed by the private key
	*/
	public boolean verify(String message, String signature, String publicKeyURI)
		throws IOException, NoSuchAlgorithmException, NoSuchProviderException,
		InvalidKeySpecException, InvalidKeyException, SignatureException
	{
		
		PublicKey pk = readPublicKey(publicKeyURI);
		return verify(message, signature, pk);
	}
	
	/**
	* verify that the message was signed by the private key by using the public key
	* @param message     the message to be verified
	* @param signature   the signature generated by the private key and encoded in Base64
	* @return boolean   true if the message was signed by the private key
	*/
	public boolean verify(String message, String signature)
		throws IOException, NoSuchAlgorithmException, NoSuchProviderException,
		InvalidKeySpecException, InvalidKeyException, SignatureException
	{
		if ( publicKey == null )
			throw new InvalidKeyException("Public Key not provided.");
		return verify( message, signature, publicKey);
	}
	
	/**
	* verify that the message was signed by the private key by using the public key
	* @param message     the message to be verified
	* @param signature   the signature generated by the private key and encoded in Base64
	* @param publicKey   the public key
	* @return boolean   true if the message was signed by the private key
	*/
	public boolean verify(String message, String signature, PublicKey publicKey)
		throws IOException, NoSuchAlgorithmException, NoSuchProviderException,
		InvalidKeySpecException, InvalidKeyException, SignatureException
	{
		Signature dsa = Signature.getInstance("SHA/DSA");
		dsa.initVerify(publicKey);
		dsa.update(message.getBytes());
		
		byte sigDec[] = Base64Coder.decode(signature.toCharArray());
		return dsa.verify(sigDec);
	}

}