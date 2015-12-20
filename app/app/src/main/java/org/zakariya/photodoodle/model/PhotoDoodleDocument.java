package org.zakariya.photodoodle.model;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import org.zakariya.doodle.model.PhotoDoodle;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

/**
 * Created by shamyl on 12/16/15.
 */
public class PhotoDoodleDocument extends RealmObject {

	private static final String TAG = PhotoDoodleDocument.class.getSimpleName();

	@PrimaryKey
	private String uuid;

	@Required
	private String name;

	@Required
	private Date creationDate;

	private Date modificationDate;

	/**
	 * Create a new PhotoDoodleDocument with UUID, name and creationDate set.
	 *
	 * @param realm the realm into which to assign the new document
	 * @param name  the name of the document, e.g., "Untitle Document"
	 * @return a new PhotoDoodleDocument with unique UUID, in the realm and ready to use
	 */
	public static PhotoDoodleDocument create(Realm realm, String name) {
		PhotoDoodleDocument doc = new PhotoDoodleDocument();
		doc.setUuid(UUID.randomUUID().toString());
		doc.setCreationDate(new Date());
		doc.setModificationDate(new Date());
		doc.setName(name);

		realm.beginTransaction();
		realm.copyToRealm(doc);
		realm.commitTransaction();

		return doc;
	}

	/**
	 * Delete the document from the Realm
	 * @param context the context
	 * @param realm the realm
	 * @param document the document to delete
	 */
	public static void delete(Context context, Realm realm, PhotoDoodleDocument document) {
		File file = getPhotoDoodleSaveFile(context, document);
		if (file.exists()) {
			//noinspection ResultOfMethodCallIgnored
			file.delete();
		}

		realm.beginTransaction();
		document.removeFromRealm();
		realm.commitTransaction();
	}

	@Nullable
	public static PhotoDoodleDocument getPhotoDoodleDocumentByUuid(Realm realm, String uuid) {
		return realm.where(PhotoDoodleDocument.class).equalTo("uuid", uuid).findFirst();
	}

	/**
	 * Get the file used by a PhotoDoodleDocument to save its doodle (the doodle is not saved in the realm because it can be big)
	 * @param context the context
	 * @param document the document
	 * @return a File object referring to the document's doodle's save data. May not exist if nothing has been saved
	 */
	public static File getPhotoDoodleSaveFile(Context context, PhotoDoodleDocument document) {
		return new File(context.getFilesDir(), document.getUuid() + ".doodle");
	}

	public static void savePhotoDoodle(Context context, PhotoDoodleDocument document, PhotoDoodle doodle) {
		File outputFile = getPhotoDoodleSaveFile(context, document);
		try {
			Log.i(TAG, "savePhotoDoodle: starting save");
			FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
			BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
			doodle.serialize(bufferedOutputStream);
			Log.i(TAG, "savePhotoDoodle: finish save");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static PhotoDoodle loadPhotoDoodle(Context context, PhotoDoodleDocument document) {
		PhotoDoodle doodle = new PhotoDoodle(context);
		File inputFile = getPhotoDoodleSaveFile(context, document);
		if (inputFile.exists()) {
			try {
				FileInputStream inputStream = new FileInputStream(inputFile);
				BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
				doodle.inflate(bufferedInputStream);
				inputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return doodle;
	}


	/**
	 * Set the document's modification date to now.
	 *
	 * @param realm the realm to act in
	 * @param doc   the document to mark modification date on
	 */
	public static void markModified(Realm realm, PhotoDoodleDocument doc) {
		realm.beginTransaction();
		doc.setModificationDate(new Date());
		realm.commitTransaction();
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public Date getModificationDate() {
		return modificationDate;
	}

	public void setModificationDate(Date modificationDate) {
		this.modificationDate = modificationDate;
	}
}
