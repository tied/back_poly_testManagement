package com.thed.zephyr.je.attachment;

public class ZAttachmentException extends Exception
{
	public ZAttachmentException(String message)
	{
		super(message);
	}

	public ZAttachmentException(Throwable cause)
	{
		super(cause);
	}

	public ZAttachmentException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
