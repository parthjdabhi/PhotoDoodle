package org.zakariya.photodoodle.model;

import android.content.Context;
import android.support.annotation.Nullable;

import org.zakariya.doodle.model.PhotoDoodle;

import java.io.InvalidObjectException;
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

	@PrimaryKey
	private String uuid;

	@Required
	private String name;

	@Required
	private Date creationDate;

	private Date modificationDate;

	private byte[] photoDoodleDocumentData;

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

	@Nullable
	public static PhotoDoodleDocument getPhotoDoodleDocumentByUuid(Realm realm, String uuid) {
		return realm.where(PhotoDoodleDocument.class).equalTo("uuid", uuid).findFirst();
	}

	/**
	 * Assign a PhotoDoodle to this PhotoDoodleDocument by serializing it to photoDoodleDocumentData byte array
	 *
	 * @param realm  the realm to act in
	 * @param doc    the document to which to assign the PhotoDoodle
	 * @param doodle the PhotoDoodle
	 */
	public static void setPhotoDoodleDocument(Realm realm, PhotoDoodleDocument doc, PhotoDoodle doodle) {
		byte[] bytes = doodle.serialize();

		realm.beginTransaction();
		doc.setPhotoDoodleDocumentData(bytes);
		doc.setModificationDate(new Date());
		realm.commitTransaction();
	}

	@Nullable
	public static PhotoDoodle getPhotoDoodleDocument(Context context, PhotoDoodleDocument doc) {
		try {
			PhotoDoodle doodle = new PhotoDoodle(context);
			doodle.inflate(doc.getPhotoDoodleDocumentData());
			return doodle;
		} catch (InvalidObjectException e) {
			e.printStackTrace();
			return null;
		}
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

	public byte[] getPhotoDoodleDocumentData() {
		return photoDoodleDocumentData;
	}

	public void setPhotoDoodleDocumentData(byte[] photoDoodleDocumentData) {
		this.photoDoodleDocumentData = photoDoodleDocumentData;
	}
}
