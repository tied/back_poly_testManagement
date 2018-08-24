package com.thed.zephyr.je.index;

public class IndexingFailureException extends RuntimeException
{
   private final int failures;

   public IndexingFailureException(final int failures)
   {
       this.failures = failures;
   }

   @Override
   public String getMessage()
   {
       return String.format("Indexing completed with %1$d errors", failures);
   }
}