import java.io.File;
import java.util.StringTokenizer;
import java.net.URLConnection;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.amazonaws.regions.Regions;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.core.util.Base64;

import java.nio.file.*;
import java.nio.file.attribute.*;

public class MultipartUploadDemo {

	private final String TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ik5FUTRRalZEUlVGQ1JqUTVPVFZGT0RjNE16QkdOVGREUVVaRlFqSkNRMEpGTXpnMFJqVXlOdyJ9.eyJzY29wZXMiOiJyZWFkLW9ubHkgcmVhZC13cml0ZSBuYXNfcmVhZF9vbmx5IG5hc19yZWFkX3dyaXRlIG5hc19vZmZsaW5lX2FjY2VzcyBuYXNfYXBwX21hbmFnZW1lbnQgZGV2aWNlX2dldCBkZXZpY2VfZ2V0X3VzZXJfZGV2aWNlcyBkZXZpY2VfYXR0YWNoIGRldmljZV9kZXRhY2ggZGV2aWNlX2dldF9hcHByb3ZhbHMgZGV2aWNlX2dldF9kZXZpY2VfdXNlcnMgZGV2aWNlX2FwcHJvdmVfdXNlcnMgZGV2aWNlX2ludml0ZV91c2VycyBkZXZpY2VfZ2V0X2ludml0YXRpb24gZGV2aWNlX2dldF91c2VyIG90YV9yZWFkX29ubHkiLCJpc3MiOiJodHRwczovL3dkYy1xYTIuYXV0aDAuY29tLyIsInN1YiI6ImF1dGgwfDU5NTQ0MTMwOTQyMDgzN2ZkMzY2YzMxYyIsImF1ZCI6IlEzVldBamt6Q1JoMFo3N3FjZ0xpTE8ycDVIVEtmUk1wIiwiZXhwIjoxNTA0MDMyMzU4LCJpYXQiOjE1MDM5NDU5NTh9.pfVTanC8VCljRC8XQ6lRreizkxcwGsV0yLdkx9b1p_jreFEGcZDdAMlNDIsEzTPBUVB3zXNMcMOlvms6Yx3bOBofQ4Kh-6BVtrF3Y9OiXA8SZX9ZIbmsvNX9zPXTJiEz1O83KDUGBPK2SxF023EcWX1Arg5Hgch5oEAM734sxHTp5wx2NcWUwv1W37Q5LZY5qm0b3O4Fh4yhy-FagUOdnfX-bTt8h1AOrfp5jVm_bAv6nr6hVyId8yGGc_9DQVDuQay0-MuMyQ6EJjSBG3-0NDQ7553J1f-cAjBMqWhQniGgi1zBB2Q4M2zSTiLMBfbAuS6fnQFPU2zyIFfIArqEuQ";

	private final String BASE_URL = "https://wdcloud.wdtest1.com/cloud";

	class Pair{
		public String key;
		public String value;
		Pair(String key, String value) {
			this.key = key;
			this.value = value;
		}
	};
	//private File sourceFile;
	public static void main (String []args)
	{
		try{
			if(args.length == 2) {
				// String currentFolderName = System.getProperty("user.dir");
				String currentFolderName = args[0];
				String uploadFolderName = args[1];
				if (currentFolderName != null){
					System.out.println("Working Directory = " + currentFolderName);
					System.out.println("Target Directory = " + uploadFolderName);
					File currentFolder = new File(currentFolderName);
					for (final File fileEntry : currentFolder.listFiles()) {
			        if (!fileEntry.isDirectory() && !fileEntry.getName().startsWith(".")) {
								String uploadJson = getFileJson(fileEntry, uploadFolderName);
									System.out.println("JSON " + uploadJson);
									MultipartUploadDemo multipartUploadDemo = new MultipartUploadDemo();
									Pair multiPartKey = multipartUploadDemo.resumableUpload(uploadJson);
									System.out.println("fileId" + multiPartKey.key);
									multipartUploadDemo.uploadToS3(multiPartKey.value, multiPartKey.key, fileEntry);
			        }
			    }
				}
		}

		/*
		boolean sourceFileExists = new File(args[0]).exists();
		if(args.length == 1 && sourceFileExists)
		{
			MultipartUploadDemo multipartUploadDemo = new MultipartUploadDemo(args[0]);
			Pair multiPartKey = multipartUploadDemo.resumableUpload();
			System.out.println("fileId" + multiPartKey.key);
			multipartUploadDemo.uploadToS3(multiPartKey.value, multiPartKey.key);
		}
		else
		{
			System.out.println("Missing source file path");
		}
		*/
	}catch(Exception e) {
		System.out.println("Something going wrong "+ e.getMessage());
	}

	}

	public static String getFileJson(File file, String parent)
	{
		StringBuffer buffer = new StringBuffer();
		buffer.append("--mycloudboundary\n\r{\"name\":\"");
		buffer.append(file.getName());
		buffer.append("\", \"size\": \"");
		buffer.append(file.length());
		buffer.append("\", \"parentID\": \"");
		buffer.append(parent);
		String mimeType = URLConnection.guessContentTypeFromName(file.getName());
		if (mimeType != null){
			buffer.append("\", \"mimeType\": \"");
			buffer.append(mimeType);
		}
		buffer.append("\"");
		try{
			BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
			System.out.println("creationTime: " + attr.creationTime());
			if (attr.creationTime() != null){
				buffer.append(", \"cTime\": \"");
				buffer.append(attr.creationTime());
				buffer.append("\"");
			}
		}catch(Exception ex){
			System.out.println("Cannot get creation time" + ex.getMessage());
		}
		buffer.append("}\n\r--mycloudboundary--'");
		String json = buffer.toString();
		return json;
	}

	// Constructor
	/*public MultipartUploadDemo(String sourceFilePath)
	{
		try
		{
			sourceFile = new File(sourceFilePath);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}*/

	public Pair resumableUpload(String json)
	{
		//String json = getFileJson();
		MediaType[] mediaType = new MediaType[]{MediaType.APPLICATION_JSON_TYPE};
		ClientConfig config = new DefaultClientConfig();
		Client client = Client.create(config);
		WebResource service = client.resource(BASE_URL);
		System.out.println("Sending request ........");
		System.out.println(BASE_URL);
		System.out.println("Request body = " + json);

		ClientResponse response =  (ClientResponse)service.path("").path("/sdk/v2/files/resumable")
					.header("Authorization", " Bearer " + TOKEN)
					.header("Content-Type", "multipart/related;boundary=mycloudboundary")
				 	.accept( mediaType)
				 	.entity(json)
			        .post(ClientResponse.class );
		System.out.println("Parsing response........");
		if (response.getStatus() == Response.Status.CREATED.getStatusCode())
		{
			//get "Location" header
			String location = (String) response.getHeaders().getFirst("Location");
			String fileId = location.split("files/")[1];
			System.out.println("File creation Response = " + location);
			System.out.println("File creation fileId = " + fileId);
			String s3Location = (String) response.getHeaders().getFirst("Content-Location");
			System.out.println("S3 upload details = " + s3Location);
			return new Pair(fileId, s3Location);
			//return s3Location;
		}
		else
		{
			 System.out.println("Error status = " + response.getStatus());
			 String textEntity = response.getEntity(String.class);
			 System.out.println("Error message = " + textEntity);
		}
		return null;
	}

	/*public String getFileJson()
	{
		//return "--mycloudboundary\n\r{\"name\":\"george.jpg\", \"mimeType\": \"image/jpeg\", \"parentID\": \"42\", \"size\": \"136072\"}\n\r--mycloudboundary--'";
		return "--mycloudboundary\n\r{\"name\":\"SL_MM_176.44.mp4\", \"mimeType\": \"video/mp4\", \"parentID\": \"351\", \"size\": \"105712\"}\n\r--mycloudboundary--'";
	}*/

	public void uploadToS3(String multipartKey, String fileId, File sourceFile)
	{
		System.out.println("S3 multipartKey: " + multipartKey);
		System.out.println("fileId" + fileId);
		// Split up the multipartKey
		StringTokenizer stringTokenizer = new StringTokenizer(multipartKey, "|");

		String multipart = stringTokenizer.nextToken();
		System.out.println("S3 multipart: " + multipart);
		String region = stringTokenizer.nextToken();
		System.out.println("S3 region: " + region);
		String bucket = stringTokenizer.nextToken();
		System.out.println("S3 bucket: " + bucket);
		String key = stringTokenizer.nextToken();
		System.out.println("S3 key: " + key);
		String accessKeyId = stringTokenizer.nextToken();
	    System.out.println("S3 accessKeyId: " + accessKeyId);
		String secretAccessKey = stringTokenizer.nextToken();
	    System.out.println("S3 secretAccessKey: " + secretAccessKey);
		String sessionToken = stringTokenizer.nextToken();
	    System.out.println("S3 sessionToken: " + sessionToken);
		sessionToken = new String(Base64.decode(sessionToken));


	    uploadFileUsingSTSToken(region, accessKeyId, secretAccessKey, sessionToken, bucket, key, fileId, sourceFile);
	}

	 public void uploadFileUsingSTSToken(String region, String accessKey,
    		String secretAccessKey,
    		String sessionToken,
    		String bucket,
    		String key,
				String fileId,
				File sourceFile
    		) {
        try {
        	BasicSessionCredentials basicSessionCredentials =
                    new BasicSessionCredentials( accessKey,
                                               secretAccessKey,
                                               sessionToken);
        	//AmazonS3 s3client = new AmazonS3Client(basicSessionCredentials);
        	AmazonS3 s3client = AmazonS3ClientBuilder.standard().withRegion(
        			region)
        			//.withForceGlobalBucketAccessEnabled(true)
        			.withCredentials(
        			new AWSStaticCredentialsProvider(basicSessionCredentials)).build();

            System.out.println("Uploading new object to S3");
            s3client.putObject(new PutObjectRequest(
            		                 bucket, key, sourceFile));
            System.out.println("Uploading successful");
        } catch (Exception ace) {
            System.out.println("Error Message: " + ace.getMessage());
            ace.printStackTrace();
        }
				updateUploadStatusToWD(fileId);
    }

		private void updateUploadStatusToWD(String fileId){
			ClientConfig config = new DefaultClientConfig();
			Client client = Client.create(config);
			String fileUrl = BASE_URL + "/sdk/v2/files/" + fileId + "/resumable/content?done=true";
			WebResource service = client.resource(fileUrl);
			System.out.println("Sending request ........");
			System.out.println(fileUrl);

			//ClientResponse response =  (ClientResponse)service.path("").path("/sdk/v2/files/" + fileId + "/resumable/content?done=true")
			ClientResponse response =  (ClientResponse)service
						.header("Authorization", " Bearer " + TOKEN)
						.put(ClientResponse.class);
			System.out.println("Parsing response........" + response.getStatus());
			if (response.getStatus() == Response.Status.CREATED.getStatusCode())
			{
				//get "Location" header
				String location = (String) response.getHeaders().getFirst("Location");
				System.out.println("File creation Response = " + location);
			}
			else
			{
				 System.out.println("Error status = " + response.getStatus());
				 String textEntity = response.getEntity(String.class);
				 System.out.println("Error message = " + textEntity);
			}
		}
}
