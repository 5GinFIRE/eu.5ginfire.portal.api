/**
 * Copyright 2017 University of Patras 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License.
 * You may obtain a copy of the License at:
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 
 * See the License for the specific language governing permissions and limitations under the License.
 */

package portal.api.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.activation.DataHandler;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.jaxrs.ext.multipart.Attachment;

/**
 * @author ctranoris
 *
 */
public class AttachmentUtil {

	/**
	 * @param name
	 * @param attachments
	 * @return
	 */
	public static String getAttachmentStringValue(String name, List<Attachment> attachments) {

		Attachment att = getAttachmentByName(name, attachments);
		if (att != null) {
			return att.getObject(String.class);
		}
		return null;
	}

	public static Attachment getAttachmentByName(String name, List<Attachment> attachments) {

		for (Attachment attachment : attachments) {
			String s = getAttachmentName(attachment.getHeaders());
			if ((s != null) && (s.equals(name)))
				return attachment;
		}

		return null;
	}

	public static List<Attachment> getListOfAttachmentsByName(String name, List<Attachment> attachments) {

		List<Attachment> la = new ArrayList<Attachment>();
		for (Attachment attachment : attachments) {
			if (getAttachmentName(attachment.getHeaders()).equals(name))
				la.add(attachment);
		}
		return la;
	}

	private static String getAttachmentName(MultivaluedMap<String, String> header) {

		if (header.getFirst("Content-Disposition") != null) {
			String[] contentDisposition = header.getFirst("Content-Disposition").split(";");
			for (String filename : contentDisposition) {
				if ((filename.trim().startsWith("name"))) {
					String[] name = filename.split("=");
					String exactFileName = name[1].trim().replaceAll("\"", "");
					return exactFileName;
				}
			}
		}
		return null;
	}

	public static String getFileName(MultivaluedMap<String, String> header) {
		String[] contentDisposition = header.getFirst("Content-Disposition").split(";");
		for (String filename : contentDisposition) {
			if ((filename.trim().startsWith("filename"))) {
				String[] name = filename.split("=");
				String exactFileName = name[1].trim().replaceAll("\"", "");
				return exactFileName;
			}
		}
		return "unknown";
	}

	/**
	 * @param att
	 * @param filePath
	 * @return
	 */
	public static String saveFile(Attachment att, String filePath) {
		DataHandler handler = att.getDataHandler();
		try {
			InputStream stream = handler.getInputStream();
			MultivaluedMap map = att.getHeaders();
			File f = new File(filePath);
			OutputStream out = new FileOutputStream(f);

			int read = 0;
			byte[] bytes = new byte[1024];
			while ((read = stream.read(bytes)) != -1) {
				out.write(bytes, 0, read);
			}
			stream.close();
			out.flush();
			out.close();
			return f.getAbsolutePath();

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param att
	 * @param filePath
	 * @return
	 * @throws IOException
	 */
	public static String saveFile(ByteArrayOutputStream att, String filePath) throws IOException {

		File f = new File(filePath);
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(f);
			att.writeTo(fos);
			fos.close();
			return f.getAbsolutePath();
		} catch (IOException ioe) {
			// Handle exception here
			ioe.printStackTrace();
		} finally {
		}

		return null;

	}
}
