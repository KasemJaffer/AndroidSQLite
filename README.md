AndroidSQLite
=============

Android Library Project for SQLite CRUD operations


AndroidSQLite is a well documented library Project intended to simplify the hassle of create, update, delete and 
get operation of Android SQLite database.

Take a look at the following usefull methods that you can try:

			public DatabaseContext(Context context, String DB_NAME)
			
			public void open()
			
			public void close()
			
			public <T> T add(T entity )
			
			public <T> List<T> addAll (List<T> entities)
			
			public <T> List<T> getAll(T entity, boolean withAllItsListFields)
			
			public <T> T find(T entity, boolean withAllItsListFields)
			
			public int delete(Object entity)
			
			public <T> T update(T entity, boolean withAllItsListFields)
			
			public <T> List<T> updateAll(List<T> entities, boolean withAllItsInnerListFields)
			
			public <T> List<T> findAll(T entity, ContentValues contentValues, boolean withAllItsListFields)	
			
			public <T> List<T> subQuery(T entityToReturn, Object fromEntity, Object whereEntity,  boolean withAllItsListFields)


Example:

	DatabaseContext dc = new DatabaseContext(this, "data.db"); // Database must be created seperately and put into the assets folder.
			dc.open();
			
			dc.add(employee);
			dc.addAll(employees);
			dc.delete(employee);
			dc.find(employee, true);
			dc.update(employee, true);
			dc.updateAll(employees, true);
			dc.getAll(new Employee(), true);
			dc.findAll(new Employee(), contentValues, true);	
			dc.subQuery(new Meeting(), new MeetingAttendee(), attendee, true);
			
			dc.close();
			
Supported variable types to store in the database:

	int
	float
	long  
	String

	If you want to store Date variable, store it as String or long(milliseconds)				

