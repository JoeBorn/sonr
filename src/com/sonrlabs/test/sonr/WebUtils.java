package com.sonrlabs.test.sonr;

import com.sonrlabs.prod.sonr.R;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * These are not used, they're just examples.
 */
class WebUtils {

   /**
    * Open a stream to a url.
    * 
    * @param url The url to open.
    * @return a stream to the gven url. Caller should close.
    * @throws ClientProtocolException if an HTTP protocol error occurs.
    * @throws IOException if a stream error occurs.
    */
   static InputStream getUrlStream(String url)
         throws ClientProtocolException, IOException {
      AbstractHttpClient client = new DefaultHttpClient();
      HttpGet method = new HttpGet(url);
      HttpResponse response = client.execute(method);
      return response.getEntity().getContent();
   }

   /**
    * Construct a string from the contents of a stream. The stream will be
    * closed after the contents have been read.
    * 
    * @param stream the stream to read.
    * @return the text in the stream.
    * @throws IOException if the read fails.
    */
   static String streamToString(InputStream stream)
         throws IOException {
      BufferedReader buffer = new BufferedReader(new InputStreamReader(stream));
      StringBuilder sb = new StringBuilder();
      String line;
      while ((line = buffer.readLine()) != null) {
         sb.append(line).append("\n");
      }
      stream.close();
      return sb.toString();
   }

}
