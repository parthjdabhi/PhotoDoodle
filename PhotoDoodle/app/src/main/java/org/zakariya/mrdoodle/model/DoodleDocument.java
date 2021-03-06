package org.zakariya.mrdoodle.model;

import android.content.Context;
import android.support.annotation.Nullable;

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
import io.realm.RealmResults;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

/**
 * Created by shamyl on 12/16/15.
 */
public class DoodleDocument extends RealmObject {

	private static final String TAG = DoodleDocument.class.getSimpleName();

	@PrimaryKey
	private String uuid;

	@Required
	private String name;

	@Required
	private Date creationDate;

	private Date modificationDate;

	/**
	 * Create a new DoodleDocument with UUID, name and creationDate set.
	 *
	 * @param realm the realm into which to assign the new document
	 * @param name  the name of the document, e.g., "Untitle Document"
	 * @return a new DoodleDocument with unique UUID, in the realm and ready to use
	 */
	public static DoodleDocument create(Realm realm, String name) {

		realm.beginTransaction();
		DoodleDocument doc = realm.createObject(DoodleDocument.class);
		doc.setUuid(UUID.randomUUID().toString());
		doc.setCreationDate(new Date());
		doc.setModificationDate(new Date());
		doc.setName(name);
		realm.commitTransaction();

		return doc;
	}

	public static RealmResults<DoodleDocument> all(Realm realm) {
		return realm.allObjects(DoodleDocument.class);
	}

	/**
	 * Delete the document from the Realm
	 *
	 * @param context  the context
	 * @param realm    the realm
	 * @param document the document to delete
	 */
	public static void delete(Context context, Realm realm, DoodleDocument document) {
		File file = getSaveFile(context, document);
		if (file.exists()) {
			//noinspection ResultOfMethodCallIgnored
			file.delete();
		}

		realm.beginTransaction();
		document.removeFromRealm();
		realm.commitTransaction();
	}

	@Nullable
	public static DoodleDocument byUUID(Realm realm, String uuid) {
		return realm.where(DoodleDocument.class).equalTo("uuid", uuid).findFirst();
	}

	/**
	 * Get the file used by a DoodleDocument to save its doodle (the doodle is not saved in the realm because it can be big)
	 *
	 * @param context  the context
	 * @param document the document
	 * @return a File object referring to the document's doodle's save data. May not exist if nothing has been saved
	 */
	public static File getSaveFile(Context context, DoodleDocument document) {
		return new File(context.getFilesDir(), document.getUuid() + ".doodle");
	}

	public static void save(Context context, DoodleDocument document, PhotoDoodle doodle) {
		File outputFile = getSaveFile(context, document);
		try {
			FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
			BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
			doodle.serialize(bufferedOutputStream);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static PhotoDoodle load(Context context, DoodleDocument document) {
		PhotoDoodle doodle = new PhotoDoodle(context);
		File inputFile = getSaveFile(context, document);
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
	public static void markModified(Realm realm, DoodleDocument doc) {
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
