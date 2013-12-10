package com.kas.androidsqlite;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.AbstractWindowedCursor;
import android.database.Cursor;
import android.database.CursorWindow;
import android.database.sqlite.SQLiteDatabase;

import com.google.gson.Gson;

/**
 * Created by Kassim on 10/3/13.
 */
public class DatabaseContext {
	
	private DBOpenHelper dbhelper;
	private SQLiteDatabase database;
	private boolean isAutoOpenAndClose;

/**
 * 
 * @param context
 * @param DB_NAME Database name in the assets folder for example data.db
 * @param isAutoOpenAndClose if set to true, it will open the database before every operation and closes it after automatically
 * @throws IOException throws exception if the DB_NAME passed not found in the assets folder.
 * @N.B BestPractice I recommend you to have one instance of this class through out your application. Hopefully, that way you won't have conflicts and everything would would work synchronously.
 */
	public DatabaseContext(Context context, String DB_NAME, boolean isAutoOpenAndClose) throws IOException {
		dbhelper = new DBOpenHelper(context, DB_NAME);
		dbhelper.createDatabase();
		this.isAutoOpenAndClose=isAutoOpenAndClose;
	}

	/**
	 * Opens the connection with the database
	 */
	public void open() {
		database = dbhelper.openDataBase();
	}

	/**
	* Closes the connection with the database
	 */
	public void close() {

		dbhelper.close();
	}

	/**
	* Adds an entity and if the entity has list of entities it will add them as well.
	    * @Forexample 	    
	    *<code> 	     
	    * public class ClassA{  
	    * <pre>
	    *	long id;
	    * 	String Name;
	    * 	List(ClassB) mylist;  <----To Add this to the database, You must have a property name in ClassB that matches the name of this class
	    * }										
	    * 
	    * public class ClassB{
	    *	long id;
	    *	String anything;
	    *	long ClassA;	<------- like this
	    *}</code>
	    *</pre>
	    * @param entity Entity to be added. The name of the class must match the name of the table in the database.
	    * @return T returns the added Entity with id assigned .
	    */
	
	public <T> T add(T entity ) {
		
		if(isAutoOpenAndClose)
			open();
		
		HashMap<String, List<Object>> externalObjects = new HashMap<String, List<Object>>();
		
		long insertid = database.insert(entity.getClass().getSimpleName(), null,getContentValues(entity, externalObjects));	
		
		setObjectField(entity,"id", insertid);
		for (Entry<String, List<Object>> entry : externalObjects.entrySet()) {
			List<?extends Object> ext = addAll(entry.getValue(), entity.getClass().getSimpleName(),insertid);
			setObjectField(entity, entry.getKey(), ext);
		}	
		
		if(isAutoOpenAndClose)
			close();
		
		return entity;
	}
	
	/**
	 * Adds an entity and if the entity has list of entities it will add them as well.
	 * @param entity Entity to be added. The name of the class must match the name of the table in the database.
	 * @param IdPropertyName pass the primary key name. This is in case the primary key is named other than "id" or "Id"
	 * @param isAutoIncrement set true if the primary key is auto-increment
	 * @return T returns the added Entity with id assigned .
	 */
	public <T> T add(T entity, String IdPropertyName, boolean isAutoIncrement) {
		
		if(isAutoOpenAndClose)
			open();
		
		HashMap<String, List<Object>> externalObjects = new HashMap<String, List<Object>>();
		
		long insertid = database.insert(entity.getClass().getSimpleName(), null, getContentValues(entity, IdPropertyName,isAutoIncrement, externalObjects));	
		
		setObjectField(entity, IdPropertyName, insertid);
		for (Entry<String, List<Object>> entry : externalObjects.entrySet()) {
			List<?extends Object> ext = addAll(entry.getValue(), entity.getClass().getSimpleName(),insertid);
			setObjectField(entity, entry.getKey(), ext);
		}
		
		if(isAutoOpenAndClose)
			close();
		
		return entity;
	}
	
	/**
	 * Adds list of entities(records) in a table in the database.
	 * @param entities to be added. The name of the class must match the name of the table in the database.
	 * @return returns list of added entities.
	 */
	public <T> List<T> addAll (List<T> entities){
		for (T entity : entities) {
			add(entity);
		}	
		return entities;		
	}
	
	public <T> List<T> addAll (List<T> entities, String IdPropertyName, boolean isAutoIncrement){
		for (T entity : entities) {
			add(entity, IdPropertyName, isAutoIncrement);
		}	
		return entities;		
	}
		
	/**
	 * Gets all the records in a table in the database.
	 * @param entity only pass instance of a class, for example new Employee(). Do not pass the class itself. The name of its class must match the name of the table in the database.
	 * @param withAllItsListFields if true, it will fill the list properties in the entity. If false, the list properties will be null.
	 * @return returns list of entities .
	 * @ForExample
	 * getAll(new Employee(), true);
	 */
	public <T> List<T> getAll(T entity, boolean withAllItsListFields) {
		
		if(isAutoOpenAndClose)
			open();
			
		Cursor cursor = database.query(entity.getClass().getSimpleName(), null, null, null, null, null, null);
		
		List<T> entitiesToReturn;		
		if(withAllItsListFields) entitiesToReturn= getDataWithAllItsReference(cursor, entity);
		else entitiesToReturn = getDataFromCursor(cursor, entity);

		if(isAutoOpenAndClose)
			close();
		
		return entitiesToReturn;
	}

	/**
	 * Finds a single record from a table in the database.
	 * @param entity pass an entity to be found. the entity must have a property named id and is assigned. The name of its class must match the name of the table in the database.
	 * @param withAllItsListFields if true, it will fill the list properties in the entity. If false, the list properties will be null.
	 * @return returns list of entities.
	 */
	public <T> T find(T entity, boolean withAllItsListFields) {

		if(isAutoOpenAndClose)
			open();

		Cursor cursor = database.query(entity.getClass().getSimpleName(), null,
					"id=?", new String[] { String.valueOf(getIdFromObject(entity)) }, null, null, null,
					null);
		
		T entityToReturn;
		if(withAllItsListFields) entityToReturn =  getDataWithAllItsReference(cursor, entity).get(0);
		else entityToReturn = getDataFromCursor(cursor, entity).get(0);
		
		if(isAutoOpenAndClose)
			close();
		
		return entityToReturn;
	}

	/**
	 * Deletes a record from a table in the database.
	 * @param entity pass an entity to be deleted. the entity must have a property named id and is assigned. The name of its class must match the name of the table in the database.
	 * @return returns the number of rows affected.
	 */
	public int delete(Object entity){
		
		if(isAutoOpenAndClose)
			open();
		
		HashMap<String, Object> iCollectionObject = new HashMap<String, Object>();		
		getObjectFields(entity, iCollectionObject).toArray(new String[] {});
		
		for (Entry<String, Object> entry : iCollectionObject.entrySet()) {
			database.delete(entry.getValue().getClass().getSimpleName(), entity.getClass().getSimpleName() +"= ?",
	                new String[] { String.valueOf(getIdFromObject(entity))});	
		}
		
		int result = database.delete(entity.getClass().getSimpleName(), "id= ?",
                new String[] { String.valueOf(getIdFromObject(entity)) });	
		
		if(isAutoOpenAndClose)
			close();
		
		return result;
	}
	
	/**
	 * Deletes a record from a table in the database.
	 * @param entity pass an entity to be deleted. the entity must have a property named id and is assigned. The name of its class must match the name of the table in the database.
	 * @param IdPropertyName pass the primary key name. This is in case the primary key is named other than "id" or "Id"
	 * @return returns the number of rows affected.
	 */
	public int delete(Object entity, String IdPropertyName){
		
		if(isAutoOpenAndClose)
			open();
		
		HashMap<String, Object> iCollectionObject = new HashMap<String, Object>();		
		getObjectFields(entity, iCollectionObject).toArray(new String[] {});
		
		for (Entry<String, Object> entry : iCollectionObject.entrySet()) {
			database.delete(entry.getValue().getClass().getSimpleName(), entity.getClass().getSimpleName() +"= ?",
	                new String[] { String.valueOf(getIdFromObject(entity))});	
		}
		
		int result = database.delete(entity.getClass().getSimpleName(), IdPropertyName+"= ?",
                new String[] { String.valueOf(getIdFromObject(entity, IdPropertyName)) });	
		
		if(isAutoOpenAndClose)
			close();
		
		return result;
	}

	/**
	 * Updates a record in a table in the database
	 * @param entity entity to be updated. the entity must have a property named id and is assigned. The name of its class must match the name of the table in the database.
	 * @param withAllItsListFields if true, it will update the list properties in the entity. If false, the list properties will be not be updated.
	 * @return returns the updated entity
	 */
	public <T> T update(T entity, boolean withAllItsListFields){
		
		if(isAutoOpenAndClose)
			open();
		
		HashMap<String, List<Object>> externalObjects = new HashMap<String, List<Object>>();
		ContentValues values = getContentValues(entity, externalObjects);
		int result = database.update(entity.getClass().getSimpleName(), values, "id = ?",
                new String[] { String.valueOf(getIdFromObject(entity)) });	
		
		if(withAllItsListFields){
			for (Entry<String, List<Object>> entry : externalObjects.entrySet()) {
				List<Object> ext = updateAll(entry.getValue(), withAllItsListFields);
				setObjectField(entity, entry.getKey(), ext);
			}
		}
		
		if(isAutoOpenAndClose)
			close();
		
		if(result == 1) return entity;
		else return setObjectField(entity, "id", 0);	
	}
	
	/**
	 * Updates a record in a table in the database
	 * @param IdPropertyName in case the primary key is name other than Id
	 * @param entity entity to be updated. the entity must have a property named id and is assigned. The name of its class must match the name of the table in the database.
	 * @param withAllItsListFields if true, it will update the list properties in the entity. If false, the list properties will be not be updated.
	 * @return returns the updated entity
	 */
	public <T> T update(T entity, String IdPropertyName,  boolean withAllItsListFields){
		
		if(isAutoOpenAndClose)
			open();
		
		HashMap<String, List<Object>> externalObjects = new HashMap<String, List<Object>>();
		ContentValues values = getContentValues(entity, externalObjects);
		int result = database.update(entity.getClass().getSimpleName(), values, IdPropertyName+" = ?",
                new String[] { String.valueOf(getIdFromObject(entity,IdPropertyName)) });	
		
		if(withAllItsListFields){
			for (Entry<String, List<Object>> entry : externalObjects.entrySet()) {
				List<Object> ext = updateAll(entry.getValue(), withAllItsListFields);
				setObjectField(entity, entry.getKey(), ext);
			}
		}
		
		if(isAutoOpenAndClose)
			close();
		
		if(result == 1) return entity;
		else return setObjectField(entity, IdPropertyName, 0);
	}
	
	/**
	 * Updates list of records in a table in the database
	 * @param IdPropertyName in case the primary key is name other than Id
	 * @param entities list of entities to be updated. All the entities must have a property named id and is assigned. The name of its class must match the name of the table in the database.
	 * @param withAllItsInnerListFields if true, it will update the inner list properties in the entity. If false, the inner list properties will be not be updated.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> List<T> updateAll(List<T> entities, String IdPropertyName, boolean withAllItsInnerListFields){
		List<Object> insertedObjects = new ArrayList<Object>();		
		for(Object obj : (List<Object>) entities){
			insertedObjects.add(update(obj,IdPropertyName, withAllItsInnerListFields));
		}
		return (List<T>) insertedObjects;
	}
	
	/**
	 * Updates list of records in a table in the database
	 * @param entities list of entities to be updated. All the entities must have a property named id and is assigned. The name of its class must match the name of the table in the database.
	 * @param withAllItsInnerListFields if true, it will update the inner list properties in the entity. If false, the inner list properties will be not be updated.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> List<T> updateAll(List<T> entities, boolean withAllItsInnerListFields){
		List<Object> insertedObjects = new ArrayList<Object>();		
		for(Object obj : (List<Object>) entities){
			insertedObjects.add(update(obj, withAllItsInnerListFields));
		}
		return (List<T>) insertedObjects;
	}

	/**
	 * Finds all records in a table matching the ContenetValues passed
	 * @param entity only pass instance of a class, for example new Employee(). Do not pass the class itself. The name of its class must match the name of the table in the database.
	 * @param contentValues the where clause. ContentValues to search based on. 
	 * @param withAllItsListFields if true, it will fill the list properties in the entity. If false, the list properties will be null.
	 * @return returns list of matching entities
	 * @ForExample findAll(new Employee(), contentValues, true);
	 * 
	 */
	
	public <T> List<T> findAll(T entity, ContentValues contentValues, boolean withAllItsListFields){
		
		if(isAutoOpenAndClose)
			open();
		
		String tableName = entity.getClass().getSimpleName();
		String where = getContentValuesAsStringSeperatedBy("AND", contentValues);
		String sql = "SELECT * FROM "+tableName+" WHERE "+where;
		Cursor cursor = database.rawQuery(sql, null);
		
		List<T> entitiesToReturn;
		if(withAllItsListFields) entitiesToReturn = getDataWithAllItsReference(cursor, entity);
		else entitiesToReturn = getDataFromCursor(cursor, entity);
		
		if(isAutoOpenAndClose)
			close();
		
		return entitiesToReturn;
	}

	/**
	 * Finds all matched records from a many to many relationship table
	 * @param entityToReturn only pass instance of a class, for example new Employee(). Do not pass the class itself. The name of its class must match the name of the table in the database.
	 * @param fromEntity only pass instance of a class, for example new Employee(). Do not pass the class itself. The name of its class must match the name of the table in the database.
	 * @param whereEntity entity only pass instance of a class, for example new Employee(). Do not pass the class itself. The name of its class must match the name of the table in the database.
	 * @param withAllItsListFields if true, it will fill the list properties in the entity. If false, the list properties will be null.
	 * @return returns list of matched entities
	 * @ForExample <pre>
	 * <code>
	 * public class Attendee{
	 * 	------
	 * }
	 * public class Meeting{
	 * 	-----
	 * }
	 * public class MeetingAttendee{
	 * 	long Attendee;
	 * 	long Meeting;
	 * }
	 * 
	 * In this case, if you want to return all the meetings of an attendee:
	 * Attendee attendee = new Attendee();
	 * attendee.setid = 2;
	 * List[Meeting] meetings = subQuery(new Meeting(), new MeetingAttendee(), attendee, true);
	 * </code>
	 * </pre>
	 */
	public <T> List<T> subQuery(T entityToReturn, Object fromEntity, Object whereEntity,  boolean withAllItsListFields){
		
		if(isAutoOpenAndClose)
			open();
		
		Object equalval = getIdFromObject(whereEntity);
		String whereKey = whereEntity.getClass().getSimpleName();
		
		String tableToReturnName = entityToReturn.getClass().getSimpleName();
		String fromObjName = fromEntity.getClass().getSimpleName();
		
		String sql = "select * from " + tableToReturnName +" where " + tableToReturnName 
				+ ".Id in (select " + fromObjName + "." + tableToReturnName + " from " + fromObjName 
				+ " where " + fromObjName + "." + whereKey + "='" + String.valueOf(equalval) + "')";

		Cursor cursor = database.rawQuery(sql, null);
	
		List<T> entitiesToReturn;
		if(withAllItsListFields) entitiesToReturn = getDataWithAllItsReference(cursor, entityToReturn);
		else entitiesToReturn = getDataFromCursor(cursor, entityToReturn);
		
		if(isAutoOpenAndClose)
			close();
		
		return entitiesToReturn;
	}
	
	/**
	 * Runs the provided SQL and returns a user friendly List over the result set.
	 * @param entityToReturn instance of the type Object to return in a List 
	 * @param SQLQuery the SQL query. The SQL string must not be ; terminated
	 * @return return List over the result set (List of the provided entityToReturn) 
	 * @ForExample <pre>
	 * <code>
	 *	String sql = "SELECT * FROM Employee";
	 *	List[Employee] employees = databaseContext.rawQuery(new Employee(),sql);
	 * </code>
	 * </pre>
	 */
	public <T> List<T> rawQuery(T entityToReturn, String SQLQuery){
		
		if(isAutoOpenAndClose)
			open();
		
		List<T> entitiesToReturn;
		Cursor cursor = database.rawQuery(SQLQuery, null);
		entitiesToReturn = getDataFromCursor(cursor, entityToReturn);
		
		if(isAutoOpenAndClose)
			close();
		
		return entitiesToReturn;
	}
	
	
	
	private <T> T add(T obj, String key, Object val) {
		HashMap<String, List<Object>> externalObjects = new HashMap<String, List<Object>>();
		ContentValues cv = getContentValues(obj, externalObjects);
		cv.put(key, String.valueOf(val));
		
		long insertid = database.insert(obj.getClass().getSimpleName(), null, cv);
		
		setObjectField(obj,"id", insertid);
		setObjectField(obj,key, val);
		for (Entry<String, List<Object>> entry : externalObjects.entrySet()) {
			List<?extends Object> ext = addAll(entry.getValue(), obj.getClass().getSimpleName(),insertid);
			setObjectField(obj, entry.getKey(), ext);
		}
		return obj;
	}
	
	@SuppressWarnings("unchecked")
	private <T> List<T> addAll(List<T> listOfTableObjects, String key, Object val){
		List<Object> insertedObjects = new ArrayList<Object>();
		
		for(Object obj : (List<Object>) listOfTableObjects){
			insertedObjects.add(add(obj,key, val));
		}	
		return (List<T>) insertedObjects;
	}
	
	@SuppressWarnings("deprecation")
	private Object getFieldValue(String colloumnName, Cursor cursor) {
		Object obj = null;
		int index = cursor.getColumnIndex(colloumnName);
		if(index==-1) return null;

		AbstractWindowedCursor abstractWindowedCursor = (AbstractWindowedCursor)cursor;
		CursorWindow cursorWindow = abstractWindowedCursor.getWindow();
		int pos = abstractWindowedCursor.getPosition();
		if (cursor.isNull(index)) {
			obj = null;
		} else if (cursorWindow.isLong(pos,index)) {
			obj = cursor.getInt(index);
		} else if (cursorWindow.isFloat(pos, index)) {
			obj = cursor.getFloat(index);
		} else if (cursorWindow.isString(pos, index)) {
			obj = cursor.getString(index);
		}
		
		
	 		
//This method is not supported in api level < 11
//		
//int type = cursor.getType(index);
//		switch (type) {
//		case Cursor.FIELD_TYPE_FLOAT:
//			obj = cursor.getFloat(index);
//			break;
//		case Cursor.FIELD_TYPE_INTEGER:
//			obj = cursor.getInt(index);
//			break;
//		case Cursor.FIELD_TYPE_NULL:
//			obj = null;
//			break;
//		case Cursor.FIELD_TYPE_STRING:
//			obj = cursor.getString(index);
//			break;
//		}
		return obj;
	}

	private List<String> getObjectFields(Object obj) {
		List<String> allFields = new ArrayList<String>();

		Field[] fields = obj.getClass().getDeclaredFields();

		for (Field field : fields) {
			allFields.add(field.getName());
		}
		return allFields;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private List<String> getObjectFields(Object obj, HashMap<String, Object> iCollectionTypeAttribute) {
		List<String> allFields = new ArrayList<String>();

		Field[] fields = obj.getClass().getDeclaredFields();

		for (Field field : fields) {
			if (Collection.class.isAssignableFrom(field.getType())){
				try {
					Class c =(Class) ((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0];
					Object iCollectionObject = c.getConstructor().newInstance();
					iCollectionTypeAttribute.put(field.getName(), iCollectionObject);
				} catch (Exception e){}		
			}else 			
				allFields.add(field.getName());
		}
		return allFields;
	}
	
	@SuppressWarnings("unchecked")
	private ContentValues getContentValues(Object obj, HashMap<String, List<Object>> externalObjects) {
		ContentValues values = new ContentValues();
		Field[] fields = obj.getClass().getDeclaredFields();
		for (Field field : fields)
			try {
				field.setAccessible(true);
				String key = field.getName();
				Object value = field.get(obj);
				
				if (!key.equalsIgnoreCase("id") && value!=null && !(value instanceof List<?>)) {								
					values.put(key, String.valueOf(value));
				}else if(value instanceof List<?>){
					externalObjects.put(key, (List<Object>) value);
				}
			} catch (IllegalAccessException e) {}
		return values;
	}
	
	@SuppressWarnings("unchecked")
	private ContentValues getContentValues(Object obj,String IdProperty, boolean isAutoIncrement,  HashMap<String, List<Object>> externalObjects) {
		ContentValues values = new ContentValues();
		Field[] fields = obj.getClass().getDeclaredFields();
		for (Field field : fields)
			try {
				field.setAccessible(true);
				String key = field.getName();
				Object value = field.get(obj);
				
				if(isAutoIncrement){
					if (!key.equalsIgnoreCase(IdProperty) && value!=null && !(value instanceof List<?>)) {								
						values.put(key, String.valueOf(value));
					}else if(value instanceof List<?>){
						externalObjects.put(key, (List<Object>) value);
					}
				}else{
					if (value!=null && !(value instanceof List<?>)) {								
						values.put(key, String.valueOf(value));
					}else if(value instanceof List<?>){
						externalObjects.put(key, (List<Object>) value);
					}
				}
			} catch (IllegalAccessException e) {}
		return values;
	}

	private Object getIdFromObject(Object obj){
	
		Field[] fields = obj.getClass().getDeclaredFields();
		for (Field field : fields)
			try {
				field.setAccessible(true);
				String key = field.getName();
				Object value = field.get(obj);	
				if (key.equalsIgnoreCase("id")) {								
					return value;
				}
			} catch (IllegalAccessException e) {}
		return null;
	}
	
	private Object getIdFromObject(Object obj, String IdPropertyName ){
		
		Field[] fields = obj.getClass().getDeclaredFields();
		for (Field field : fields)
			try {
				field.setAccessible(true);
				String key = field.getName();
				Object value = field.get(obj);	
				if (key.equalsIgnoreCase(IdPropertyName)) {								
					return value;
				}
			} catch (IllegalAccessException e) {}
		return null;
	}
	

	private <T> T setObjectField(T obj, String Key, Object val){
		Field[] fields  = obj.getClass().getDeclaredFields();

		for (Field field : fields) {
			field.setAccessible(true);
			String keey = field.getName();
			if(keey.equalsIgnoreCase(Key)){
				try {
					field.set(obj, val);
				} catch (Exception e){}
			}
		}
		return obj;
	}

	@SuppressWarnings("unchecked")
	private <T> List<T> getDataFromCursor(Cursor cursor, T ofTypeObject){
		
		List<Object> jArray = new ArrayList<Object>();
		String[] allColloumns = getObjectFields(ofTypeObject).toArray(new String[] {});
		if (cursor.getCount() > 0) {
			while (cursor.moveToNext()) {
				JSONObject jObject = new JSONObject();
				for (String col : allColloumns) {
					try {
						jObject.put(col, getFieldValue(col, cursor));
					} catch (JSONException e) {
					}
				}
				jArray.add(new Gson().fromJson(jObject.toString(), ofTypeObject.getClass()));
			}
		}
		cursor.close();
		return (List<T>) jArray;
	}

	@SuppressWarnings("unchecked")
	private <T> List<T> getDataWithAllItsReference(Cursor cursor, T ofTypeObject){
		
		List<Object> jArray = new ArrayList<Object>();
		HashMap<String, Object> iCollectionObject = new HashMap<String, Object>();		
		String[] allColloumns = getObjectFields(ofTypeObject, iCollectionObject).toArray(new String[] {});
		
		if (cursor.getCount() > 0) {
			while (cursor.moveToNext()) {
				JSONObject jObject = new JSONObject();				
				for (String col : allColloumns) {
					try {
						jObject.put(col, getFieldValue(col, cursor));
					} catch (JSONException e) {}
				}
				Object OBJ = new Gson().fromJson(jObject.toString(), ofTypeObject.getClass());
				for (Entry<String, Object> entry : iCollectionObject.entrySet()) {
					try {
						ContentValues cv = new ContentValues();
						cv.put(ofTypeObject.getClass().getSimpleName(), jObject.getLong("id"));
						List<Object> ext = findAll(entry.getValue(), cv, true );
						setObjectField(OBJ, entry.getKey(), ext);
					} catch (JSONException e) {}
				}
				jArray.add(OBJ);
			}
		}
		cursor.close();		
		return (List<T>) jArray;	
	}
	
	private String getContentValuesAsStringSeperatedBy(String speratedBy, ContentValues contentValues) {	
		String c = contentValues.valueSet().toString();	
		return c.replace("[", "").replace("=", "='").replaceAll(",","' "+speratedBy).replace("]", "'");
	}
	

}
