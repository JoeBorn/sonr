package com.sonrlabs.test.sonr.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.content.SharedPreferences;

public final class Common {

   public static final String N_A = "N_A";
   private static final String SHARED_PREF_NAME = "SONR";

   private Common() {
      // utility
   }

   /**
    * Converts from {@code String listOfStringBooleans = "true, false, false" } to
    * {@code boolean[] booleanArray = { true, false, false }};
    *
    * @note	mostly used for converting from database retrieved string to
    * boolean[] used in selected options on certain Android widgets.
    *
    * @param str string of form "true, false, false"
    *
    */
   public static boolean[] toBooleanArray(String str) {
      String[] flagStrings = str.split(",");
      boolean[] flags = new boolean[flagStrings.length];
      for (int i=0; i<flagStrings.length; i++) {
         String flagString = flagStrings[i].trim();
         flags[i] = "true".equalsIgnoreCase(flagString);
      }
      return flags;
   }

   // ---------------------------------------------------------------------

//    public static void saveValue(Context c, RequestToken type,
//	    String strValue) {
//	SharedPreferences.Editor editor = c.getSharedPreferences(
//		Common.sharedPrefName, Context.MODE_PRIVATE).edit();
//	editor.putString(type.name(), strValue);
//	editor.commit();
//    }

   /**
    * Removes a specific value from a saved boolean array...
    *
    * @param c			application context
    * @param arrayId		array which will be used for key (corresponding to R.array.id)
    * @param whichValue	which value to remove
    */
   public static void removeValue(Context c, int arrayId, int whichValue) {
      SharedPreferences.Editor editor = c.getSharedPreferences(Common.SHARED_PREF_NAME, Context.MODE_PRIVATE).edit();
      editor.remove(String.format("%s.%s", arrayId, whichValue));
      editor.commit();
   }

   /**
    * Retrieves a boolean array from SharedPreferences.
    * If a value was not in the array, false will be in its place.
    *
    * Returns null if not even one boolean is found to be true.
    *
    * @param c			application context
    * @param arrayId		R.array.arrayId for easier put/get
    * @param vals		actual array
    */
   public static boolean[] getValue(Context c, int arrayId) {
      boolean atLeastOneTrue = false;

      String[] optionStrings = c.getResources().getStringArray(arrayId);
      boolean[] response = new boolean[optionStrings.length];
      SharedPreferences settings = c.getSharedPreferences(Common.SHARED_PREF_NAME, Context.MODE_PRIVATE);
      for (int i = 0; i < optionStrings.length; i++) {
         boolean storedValue = settings.getBoolean(String.format("%s.%s", arrayId, i), false);
         if (!atLeastOneTrue && storedValue) {
            atLeastOneTrue = true;
         }
         response[i] = storedValue;
      }
      return atLeastOneTrue ? response : null;
   }

   public static void save(Context c, String key, String value) {
      SharedPreferences.Editor editor = c.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE).edit();
      editor.putString(key, value);
      editor.commit();
   }

   public static String get(Context c, String key, String defaultValue) {
      SharedPreferences settings = c.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
      return settings.getString(key, defaultValue);
   }

   public static void save(Context c, String key, boolean value) {
      SharedPreferences.Editor editor = c.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE).edit();
      editor.putBoolean(key, value);
      editor.commit();
   }

   public static boolean get(Context c, String key, boolean defaultValue) {
      SharedPreferences settings = c.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
      return settings.getBoolean(key, defaultValue);
   }

   /**
    * Stores boolean array to SharedPreferences.
    *
    * @param c			application context
    * @param arrayId		R.array.arrayId for easier put/get
    * @param vals		actual array
    */
   public static void saveValue(Context c, int arrayId, boolean[] vals) {
      SharedPreferences.Editor editor = c.getSharedPreferences(Common.SHARED_PREF_NAME, Context.MODE_PRIVATE).edit();
      for (int i = 0; i < vals.length; i++) {
         editor.putBoolean(String.format("%s.%s", arrayId, i), vals[i]);
      }
      editor.commit();
   }

//    public static void saveValue(Context c, RequestToken type,
//	    int intValue) {
//	SharedPreferences.Editor editor = c.getSharedPreferences(
//		Common.sharedPrefName, Context.MODE_PRIVATE).edit();
//	editor.putInt(type.name(), intValue);
//	editor.commit();
//    }

   //--------------------------------------------------------------------

   public static InputStream makeGetRequestReturnInStream(String url) {
      InputStream data;
      try {

//	    //credit: http://stackoverflow.com/questions/2703161/apache-httpclient-4-0-ignore-ssl-certificate-errors
//	    SSLContext sslContext = SSLContext.getInstance("TLS");
//
//	    // set up a TrustManager that trusts everything
//	    sslContext.init(null, new TrustManager[] { new X509TrustManager() {
//		public X509Certificate[] getAcceptedIssuers() {
//		    System.out.println("getAcceptedIssuers =============");
//		    return null;
//		}
//
//		public void checkClientTrusted(X509Certificate[] certs,
//			String authType) {
//		    System.out.println("checkClientTrusted =============");
//		}
//
//		public void checkServerTrusted(X509Certificate[] certs,
//			String authType) {
//		    System.out.println("checkServerTrusted =============");
//		}
//	    } }, new SecureRandom());
//
//	    javax.net.ssl.SSLSocketFactory sf = sslContext.getSocketFactory();
//	    
//	    
//	    Scheme httpsScheme = new Scheme("https", sf, 443);
//	    SchemeRegistry schemeRegistry = new SchemeRegistry();
//	    schemeRegistry.register(httpsScheme);
//
//	    HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
//	    
//	    HttpParams params = new BasicHttpParams();
//	    ClientConnectionManager cm = new SingleClientConnManager(params, schemeRegistry);
//	    //HttpClient              httpClient = new DefaultHttpClient(cm, params);


//	    DefaultHttpClient client = new DefaultHttpClient(cm, params);

         DefaultHttpClient client = new DefaultHttpClient();
         HttpGet method = new HttpGet(url);
         HttpResponse response = client.execute(method);
         data = response.getEntity().getContent();
      } catch (IOException e) {
         e.printStackTrace();
         data = new StringBasedInputReader("");
      }
      return data;
   }

   /**
    * make get requests return response as string...
    */
   public static String makeGetRequestReturnString(String url) {
      return generateString(makeGetRequestReturnInStream(url));
   }

   /**
    * Make get request return with string
    * @param stream
    * @return
    */
   public static String generateString(InputStream stream) {
      BufferedReader buffer = new BufferedReader(new InputStreamReader(stream));
      StringBuilder sb = new StringBuilder();

      try {
         String cur;
         while ((cur = buffer.readLine()) != null) {
            sb.append(cur).append("\n");
         }
      } catch (IOException e) {
         e.printStackTrace();
      }

      try {
         stream.close();
      } catch (IOException e) {
         e.printStackTrace();
      }
      return sb.toString();
   }

   public static String nonNullString(String strToCheck,  String strToReturnIfNull) {
      if (strToCheck != null) {
         return strToCheck;
      } else {
         return strToReturnIfNull;
      }
   }

   private static class StringBasedInputReader extends InputStream {

      private String mValue = null;

      public StringBasedInputReader(String newValue) {
         mValue = newValue;
      }

      @Override
      public int read() throws IOException {
         return 0;
      }

      @Override
      public String toString() {
         return mValue;
      }

   }

}
