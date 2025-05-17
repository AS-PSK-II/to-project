package pl.kielce.tu.orm.repository.impl;

import org.slf4j.LoggerFactory;
import pl.kielce.tu.orm.annotations.Entity;
import pl.kielce.tu.orm.annotations.ManyToMany;
import pl.kielce.tu.orm.annotations.ManyToOne;
import pl.kielce.tu.orm.annotations.OneToMany;
import pl.kielce.tu.orm.annotations.OneToOne;
import pl.kielce.tu.orm.sql.SQLAnnotationsHelper;
import pl.kielce.tu.orm.sql.SQLNamesHelper;
import pl.kielce.tu.orm.connector.DatabaseConnector;
import pl.kielce.tu.orm.repository.CrudRepository;
import pl.kielce.tu.orm.sql.SQLGenerator;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

public class CrudRepositoryImpl<T, ID> implements CrudRepository<T, ID> {
    private static final Logger logger = Logger.getLogger(CrudRepositoryImpl.class.getName());

    private static final ThreadLocal<Set<Object>> PROCESSED_ENTITIES = ThreadLocal.withInitial(HashSet::new);
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(CrudRepositoryImpl.class);

    private final Class<T> entityClass;
    private final String tableName;
    private final Field idField;
    private final List<Field> fields;
    private final DatabaseConnector databaseConnector;

    public CrudRepositoryImpl(Class<T> entityClass) {
        this.entityClass = entityClass;
        this.databaseConnector = DatabaseConnector.getInstance();

        if (!entityClass.isAnnotationPresent(Entity.class)) {
            throw new IllegalArgumentException("Class " + entityClass.getName() + " is not an entity");
        }

        Entity entityAnnotation = entityClass.getAnnotation(Entity.class);
        SQLNamesHelper sqlNamesHelper = new SQLNamesHelper(entityClass.getName());
        this.tableName = sqlNamesHelper.getTableName(entityClass, entityAnnotation.name());

        this.idField = SQLGenerator.getIdField(entityClass);

        this.fields = SQLGenerator.getFields(entityClass);
    }

    @Override
    public T save(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity must not be null");
        }

        try {
            idField.setAccessible(true);
            ID id = (ID) idField.get(entity);

            T result;
            if (id != null && existsById(id)) {
                result = update(entity);
            } else {
                result = insert(entity);
            }

            if (PROCESSED_ENTITIES.get().isEmpty()) {
                PROCESSED_ENTITIES.remove();
            }

            return result;
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error accessing ID field", e);
        }
    }

    private T insert(T entity) {
        Connection connection = databaseConnector.getConnection();
        String sql = SQLGenerator.generateInsertSQL(entity, tableName, fields);

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int paramIndex = 1;

            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(entity);

                if (value != null) {
                    if (SQLAnnotationsHelper.hasForeignTableAnnotation(field)) {
                        if (SQLAnnotationsHelper.hasOneToOneAnnotation(field) || field.isAnnotationPresent(ManyToOne.class)) {
                            Field idField = SQLGenerator.getIdField(value.getClass());
                            idField.setAccessible(true);
                            Object idValue = idField.get(value);

                            if (idValue != null) {
                                statement.setObject(paramIndex++, idValue);
                            }
                        }
                    } else if (!SQLAnnotationsHelper.hasOneToManyAnnotation(field)) {
                        statement.setObject(paramIndex++, value);
                    }
                }
            }

            int rowsAffected = statement.executeUpdate();

            if (rowsAffected > 0) {
                ResultSet generatedKeys = statement.getGeneratedKeys();
                if (generatedKeys.next()) {
                    idField.setAccessible(true);
                    idField.set(entity, generatedKeys.getObject(1));
                }

                saveRelationships(entity);
            }

            return entity;
        } catch (SQLException | IllegalAccessException e) {
            throw new RuntimeException("Error inserting entity", e);
        }
    }

    private T update(T entity) {
        Connection connection = databaseConnector.getConnection();
        String sql = SQLGenerator.generateUpdateSQL(tableName, fields, idField);

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int paramIndex = 1;

            for (Field field : fields) {
                if (!field.equals(idField)) {
                    field.setAccessible(true);
                    Object value = field.get(entity);

                    if (value != null) {
                        if (SQLAnnotationsHelper.hasForeignTableAnnotation(field)) {
                            if (SQLAnnotationsHelper.hasOneToOneAnnotation(field) || field.isAnnotationPresent(ManyToOne.class)) {
                                Field idField = SQLGenerator.getIdField(value.getClass());
                                idField.setAccessible(true);
                                Object idValue = idField.get(value);

                                if (idValue != null) {
                                    statement.setObject(paramIndex++, idValue);
                                }
                            }
                        } else if (!SQLAnnotationsHelper.hasOneToManyAnnotation(field)) {
                            statement.setObject(paramIndex++, value);
                        }
                    }
                }
            }

            idField.setAccessible(true);
            statement.setObject(paramIndex, idField.get(entity));

            statement.executeUpdate();

            saveRelationships(entity);

            return entity;
        } catch (SQLException | IllegalAccessException e) {
            throw new RuntimeException("Error updating entity", e);
        }
    }

    private void saveRelationships(T entity) throws IllegalAccessException {
        for (Field field : fields) {
            field.setAccessible(true);
            Object value = field.get(entity);

            if (value != null) {
                if (field.isAnnotationPresent(OneToOne.class)) {
                    saveOneToOneRelationship(entity, field, value);
                } else if (field.isAnnotationPresent(OneToMany.class)) {
                    saveOneToManyRelationship(entity, field, value);
                } else if (field.isAnnotationPresent(ManyToOne.class)) {
                    saveManyToOneRelationship(entity, field, value);
                } else if (field.isAnnotationPresent(ManyToMany.class)) {
                    saveManyToManyRelationship(entity, field, value);
                }
            }
        }
    }

    private void saveOneToOneRelationship(T entity, Field field, Object value) throws IllegalAccessException {
        if (PROCESSED_ENTITIES.get().contains(entity)) {
            return;
        }

        PROCESSED_ENTITIES.get().add(entity);

        try {
            OneToOne oneToOne = field.getAnnotation(OneToOne.class);
            Class<?> targetEntityClass = oneToOne.entity();

            Field entityIdField = SQLGenerator.getIdField(entityClass);
            entityIdField.setAccessible(true);
            Object entityId = entityIdField.get(entity);

            Field targetIdField = SQLGenerator.getIdField(targetEntityClass);
            targetIdField.setAccessible(true);
            Object targetId = targetIdField.get(value);

            Constructor<?> constructor = CrudRepositoryImpl.class.getConstructor(Class.class);
            Object targetRepository = constructor.newInstance(targetEntityClass);

            java.lang.reflect.Method saveMethod = CrudRepositoryImpl.class.getMethod("save", Object.class);

            saveMethod.invoke(targetRepository, value);

            for (Field targetField : targetEntityClass.getDeclaredFields()) {
                if (targetField.isAnnotationPresent(OneToOne.class)) {
                    OneToOne targetOneToOne = targetField.getAnnotation(OneToOne.class);
                    if (targetOneToOne.entity().equals(entityClass)) {
                        targetField.setAccessible(true);
                        targetField.set(value, entity);
                        saveMethod.invoke(targetRepository, value);
                        break;
                    }
                }
            }

        } catch (NoSuchMethodException | InstantiationException | InvocationTargetException e) {
            throw new RuntimeException("Error saving one-to-one relationship", e);
        } finally {
            PROCESSED_ENTITIES.get().remove(entity);
        }
    }

    private void saveOneToManyRelationship(T entity, Field field, Object value) throws IllegalAccessException {
        if (!(value instanceof Collection)) {
            return;
        }

        if (PROCESSED_ENTITIES.get().contains(entity)) {
            return;
        }

        PROCESSED_ENTITIES.get().add(entity);

        try {
            OneToMany oneToMany = field.getAnnotation(OneToMany.class);
            Class<?> targetEntityClass = oneToMany.entity();
            String mappedBy = oneToMany.mappedBy();

            Field entityIdField = SQLGenerator.getIdField(entityClass);
            entityIdField.setAccessible(true);
            Object entityId = entityIdField.get(entity);

            Constructor<?> constructor = CrudRepositoryImpl.class.getConstructor(Class.class);
            Object targetRepository = constructor.newInstance(targetEntityClass);

            java.lang.reflect.Method saveMethod = CrudRepositoryImpl.class.getMethod("save", Object.class);

            for (Object targetEntity : (Collection<?>) value) {
                if (!mappedBy.isEmpty()) {
                    try {
                        Field mappedByField = targetEntityClass.getDeclaredField(mappedBy);
                        mappedByField.setAccessible(true);
                        mappedByField.set(targetEntity, entity);
                    } catch (NoSuchFieldException e) {
                        throw new RuntimeException("Mapped field not found: " + mappedBy, e);
                    }
                }

                saveMethod.invoke(targetRepository, targetEntity);
            }
        } catch (NoSuchMethodException | InstantiationException | InvocationTargetException e) {
            throw new RuntimeException("Error saving one-to-many relationship", e);
        } finally {
            PROCESSED_ENTITIES.get().remove(entity);
        }
    }

    private void saveManyToOneRelationship(T entity, Field field, Object value) throws IllegalAccessException {
        if (PROCESSED_ENTITIES.get().contains(entity)) {
            return;
        }

        PROCESSED_ENTITIES.get().add(entity);

        try {
            ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
            Class<?> targetEntityClass = manyToOne.entity();

            Constructor<?> constructor = CrudRepositoryImpl.class.getConstructor(Class.class);
            Object targetRepository = constructor.newInstance(targetEntityClass);

            java.lang.reflect.Method saveMethod = CrudRepositoryImpl.class.getMethod("save", Object.class);
            saveMethod.invoke(targetRepository, value);
        } catch (NoSuchMethodException | InstantiationException | InvocationTargetException e) {
            throw new RuntimeException("Error saving many-to-one relationship", e);
        } finally {
            PROCESSED_ENTITIES.get().remove(entity);
        }
    }

    private void saveManyToManyRelationship(T entity, Field field, Object value) throws IllegalAccessException {
        if (!(value instanceof Collection)) {
            return;
        }

        if (PROCESSED_ENTITIES.get().contains(entity)) {
            return;
        }

        PROCESSED_ENTITIES.get().add(entity);

        try {
            ManyToMany manyToMany = field.getAnnotation(ManyToMany.class);
            Class<?> targetEntityClass = manyToMany.entity();

            Field entityIdField = SQLGenerator.getIdField(entityClass);
            entityIdField.setAccessible(true);
            Object entityId = entityIdField.get(entity);

            Constructor<?> constructor = CrudRepositoryImpl.class.getConstructor(Class.class);
            Object targetRepository = constructor.newInstance(targetEntityClass);

            java.lang.reflect.Method saveMethod = CrudRepositoryImpl.class.getMethod("save", Object.class);

            Entity entityAnnotation = entityClass.getAnnotation(Entity.class);
            Entity targetEntityAnnotation = targetEntityClass.getAnnotation(Entity.class);

            SQLNamesHelper sqlNamesHelper = new SQLNamesHelper(entityClass.getName());
            SQLNamesHelper targetSqlNamesHelper = new SQLNamesHelper(targetEntityClass.getName());

            String entityTableName = sqlNamesHelper.getTableName(entityClass, entityAnnotation.name());
            String targetTableName = targetSqlNamesHelper.getTableName(targetEntityClass, targetEntityAnnotation.name());

            List<String> tableNames = new ArrayList<>();
            tableNames.add(entityTableName);
            tableNames.add(targetTableName);
            Collections.sort(tableNames);

            String junctionTableName = tableNames.get(0) + "_" + tableNames.get(1);

            Connection connection = databaseConnector.getConnection();
            String deleteSQL = "DELETE FROM " + junctionTableName + " WHERE " + 
                               entityTableName.toLowerCase() + "_id = ?";

            try (PreparedStatement deleteStatement = connection.prepareStatement(deleteSQL)) {
                deleteStatement.setObject(1, entityId);
                deleteStatement.executeUpdate();
            } catch (SQLException e) {
                logger.warning("Error clearing existing relationships: " + e.getMessage());
            }

            for (Object targetEntity : (Collection<?>) value) {
                saveMethod.invoke(targetRepository, targetEntity);

                Field targetIdField = SQLGenerator.getIdField(targetEntityClass);
                targetIdField.setAccessible(true);
                Object targetId = targetIdField.get(targetEntity);

                if (targetId != null) {
                    String insertSQL = "INSERT INTO " + junctionTableName +
                                      " (" + entityTableName.toLowerCase() + "_id, " + 
                                      targetTableName.toLowerCase() + "_id) VALUES (?, ?)";

                    try (PreparedStatement insertStatement = connection.prepareStatement(insertSQL)) {
                        insertStatement.setObject(1, entityId);
                        insertStatement.setObject(2, targetId);
                        insertStatement.executeUpdate();
                    } catch (SQLException e) {
                        throw new RuntimeException("Error inserting into junction table", e);
                    }
                }
            }
        } catch (NoSuchMethodException | InstantiationException | InvocationTargetException e) {
            throw new RuntimeException("Error saving many-to-many relationship", e);
        } finally {
            PROCESSED_ENTITIES.get().remove(entity);
        }
    }

    @Override
    public List<T> saveAll(Iterable<T> entities) {
        if (entities == null) {
            throw new IllegalArgumentException("Entities must not be null");
        }

        List<T> result = new ArrayList<>();
        for (T entity : entities) {
            result.add(save(entity));
        }
        return result;
    }

    @Override
    public Optional<T> findById(ID id) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null");
        }

        Connection connection = databaseConnector.getConnection();
        String sql = SQLGenerator.generateSelectByIdSQL(tableName, fields, idField);

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                T entity = createEntityFromResultSet(resultSet);
                loadRelationships(entity);
                return Optional.of(entity);
            } else {
                return Optional.empty();
            }
        } catch (SQLException | ReflectiveOperationException e) {
            throw new RuntimeException("Error finding entity by ID", e);
        }
    }

    @Override
    public boolean existsById(ID id) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null");
        }

        Connection connection = databaseConnector.getConnection();
        String sql = "SELECT 1 FROM " + tableName + " WHERE " + 
                     new SQLNamesHelper(entityClass.getName()).getColumnName(idField, "") + " = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);

            ResultSet resultSet = statement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            throw new RuntimeException("Error checking if entity exists", e);
        }
    }

    @Override
    public List<T> findAll() {
        Connection connection = databaseConnector.getConnection();
        String sql = SQLGenerator.generateSelectSQL(tableName, fields);

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            ResultSet resultSet = statement.executeQuery();

            List<T> result = new ArrayList<>();
            while (resultSet.next()) {
                T entity = createEntityFromResultSet(resultSet);
                loadRelationships(entity);
                result.add(entity);
            }

            return result;
        } catch (SQLException | ReflectiveOperationException e) {
            throw new RuntimeException("Error finding all entities", e);
        }
    }

    @Override
    public List<T> findAllById(Iterable<ID> ids) {
        if (ids == null) {
            throw new IllegalArgumentException("IDs must not be null");
        }

        List<T> result = new ArrayList<>();
        for (ID id : ids) {
            findById(id).ifPresent(result::add);
        }
        return result;
    }

    @Override
    public long count() {
        Connection connection = databaseConnector.getConnection();
        String sql = SQLGenerator.generateCountSQL(tableName);

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getLong(1);
            } else {
                return 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error counting entities", e);
        }
    }

    @Override
    public void deleteById(ID id) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null");
        }

        Connection connection = databaseConnector.getConnection();
        String sql = SQLGenerator.generateDeleteSQL(tableName, idField);

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting entity by ID", e);
        }
    }

    @Override
    public void delete(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity must not be null");
        }

        try {
            idField.setAccessible(true);
            ID id = (ID) idField.get(entity);

            if (id != null) {
                deleteById(id);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error accessing ID field", e);
        }
    }

    @Override
    public void deleteAllById(Iterable<ID> ids) {
        if (ids == null) {
            throw new IllegalArgumentException("IDs must not be null");
        }

        for (ID id : ids) {
            deleteById(id);
        }
    }

    @Override
    public void deleteAll(Iterable<T> entities) {
        if (entities == null) {
            throw new IllegalArgumentException("Entities must not be null");
        }

        for (T entity : entities) {
            delete(entity);
        }
    }

    @Override
    public void deleteAll() {
        Connection connection = databaseConnector.getConnection();
        String sql = SQLGenerator.generateDeleteAllSQL(tableName);

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting all entities", e);
        }
    }

    private T createEntityFromResultSet(ResultSet resultSet) throws SQLException, ReflectiveOperationException {
        Constructor<T> constructor = entityClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        T entity = constructor.newInstance();

        for (Field field : fields) {
            if (!SQLAnnotationsHelper.hasForeignTableAnnotation(field) && !SQLAnnotationsHelper.hasOneToManyAnnotation(field)) {
                field.setAccessible(true);
                SQLNamesHelper sqlNamesHelper = new SQLNamesHelper(entityClass.getName());
                String columnName = sqlNamesHelper.getColumnName(field, "");
                Object value = resultSet.getObject(columnName);

                if (value != null) {
                    if (field.getType() == Double.class && value instanceof Float) {
                        field.set(entity, ((Float) value).doubleValue());
                    } else {
                        field.set(entity, value);
                    }
                }
            }
        }

        return entity;
    }

    private void loadRelationships(T entity) throws IllegalAccessException {
        for (Field field : fields) {
            if (SQLAnnotationsHelper.hasForeignTableAnnotation(field)) {
                field.setAccessible(true);

                if (field.isAnnotationPresent(OneToOne.class)) {
                    loadOneToOneRelationship(entity, field);
                } else if (field.isAnnotationPresent(OneToMany.class)) {
                    loadOneToManyRelationship(entity, field);
                } else if (field.isAnnotationPresent(ManyToOne.class)) {
                    loadManyToOneRelationship(entity, field);
                } else if (field.isAnnotationPresent(ManyToMany.class)) {
                    loadManyToManyRelationship(entity, field);
                }
            }
        }
    }

    private void loadOneToOneRelationship(T entity, Field field) throws IllegalAccessException {
        if (PROCESSED_ENTITIES.get().contains(entity)) {
            return;
        }

        PROCESSED_ENTITIES.get().add(entity);

        try {
            OneToOne oneToOne = field.getAnnotation(OneToOne.class);
            Class<?> targetEntityClass = oneToOne.entity();

            Field entityIdField = SQLGenerator.getIdField(entityClass);
            entityIdField.setAccessible(true);
            Object entityId = entityIdField.get(entity);

            Constructor<?> constructor = CrudRepositoryImpl.class.getConstructor(Class.class);
            Object targetRepository = constructor.newInstance(targetEntityClass);

            Entity entityAnnotation = entityClass.getAnnotation(Entity.class);
            Entity targetEntityAnnotation = targetEntityClass.getAnnotation(Entity.class);

            SQLNamesHelper sqlNamesHelper = new SQLNamesHelper(entityClass.getName());
            SQLNamesHelper targetSqlNamesHelper = new SQLNamesHelper(targetEntityClass.getName());

            String entityTableName = sqlNamesHelper.getTableName(entityClass, entityAnnotation.name());
            String targetTableName = targetSqlNamesHelper.getTableName(targetEntityClass, targetEntityAnnotation.name());

            Field targetReferenceField = null;
            for (Field targetField : targetEntityClass.getDeclaredFields()) {
                if (targetField.isAnnotationPresent(OneToOne.class)) {
                    OneToOne targetOneToOne = targetField.getAnnotation(OneToOne.class);
                    if (targetOneToOne.entity().equals(entityClass)) {
                        targetReferenceField = targetField;
                        break;
                    }
                }
            }

            if (targetReferenceField != null) {
                Connection connection = databaseConnector.getConnection();
                String columnName = sqlNamesHelper.getColumnName(targetReferenceField, "");
                String querySQL = "SELECT id FROM " + targetTableName + 
                                 " WHERE " + entityTableName.toLowerCase() + "_id = ?";

                Object targetId = null;
                try (PreparedStatement statement = connection.prepareStatement(querySQL)) {
                    statement.setObject(1, entityId);
                    ResultSet resultSet = statement.executeQuery();

                    if (resultSet.next()) {
                        targetId = resultSet.getObject(1);
                    }
                } catch (SQLException e) {
                }

                if (targetId != null) {
                    java.lang.reflect.Method findByIdMethod = CrudRepositoryImpl.class.getDeclaredMethod("findByIdWithoutRelationships", Object.class);
                    findByIdMethod.setAccessible(true);
                    Optional<?> optionalTargetEntity = (Optional<?>) findByIdMethod.invoke(targetRepository, targetId);

                    if (optionalTargetEntity.isPresent()) {
                        field.set(entity, optionalTargetEntity.get());
                    }
                } else {
                    List<Object> targetIds = new ArrayList<>();

                    String allIdsSQL = "SELECT id FROM " + targetTableName;
                    try (PreparedStatement statement = connection.prepareStatement(allIdsSQL)) {
                        ResultSet resultSet = statement.executeQuery();
                        while (resultSet.next()) {
                            targetIds.add(resultSet.getObject(1));
                        }
                    } catch (SQLException e) {
                        throw new RuntimeException("Error querying target entities", e);
                    }

                    if (!targetIds.isEmpty()) {
                        List<?> targetEntities = findAllByIdWithoutRelationships(targetRepository, targetEntityClass, targetIds);

                        for (Object targetEntity : targetEntities) {
                            targetReferenceField.setAccessible(true);
                            Object targetFieldValue = targetReferenceField.get(targetEntity);

                            if (targetFieldValue != null) {
                                Field targetFieldIdField = SQLGenerator.getIdField(entityClass);
                                targetFieldIdField.setAccessible(true);
                                Object targetFieldId = targetFieldIdField.get(targetFieldValue);

                                if (entityId.equals(targetFieldId)) {
                                    field.set(entity, targetEntity);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        } catch (NoSuchMethodException | InstantiationException | InvocationTargetException e) {
            throw new RuntimeException("Error loading one-to-one relationship", e);
        } finally {
            PROCESSED_ENTITIES.get().remove(entity);
        }
    }

    private void loadOneToManyRelationship(T entity, Field field) throws IllegalAccessException {
        if (PROCESSED_ENTITIES.get().contains(entity)) {
            return;
        }

        PROCESSED_ENTITIES.get().add(entity);

        try {
            OneToMany oneToMany = field.getAnnotation(OneToMany.class);
            Class<?> targetEntityClass = oneToMany.entity();
            String mappedBy = oneToMany.mappedBy();

            Field entityIdField = SQLGenerator.getIdField(entityClass);
            entityIdField.setAccessible(true);
            Object entityId = entityIdField.get(entity);

            Constructor<?> constructor = CrudRepositoryImpl.class.getConstructor(Class.class);
            Object targetRepository = constructor.newInstance(targetEntityClass);

            Entity targetEntityAnnotation = targetEntityClass.getAnnotation(Entity.class);
            SQLNamesHelper targetSqlNamesHelper = new SQLNamesHelper(targetEntityClass.getName());
            String targetTableName = targetSqlNamesHelper.getTableName(targetEntityClass, targetEntityAnnotation.name());

            Connection connection = databaseConnector.getConnection();
            List<Object> targetIds = new ArrayList<>();
            String allIdsSQL = "SELECT id FROM " + targetTableName;
            try (PreparedStatement statement = connection.prepareStatement(allIdsSQL)) {
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    targetIds.add(resultSet.getObject(1));
                }
            } catch (SQLException e) {
                throw new RuntimeException("Error querying target entities", e);
            }

            if (targetIds.isEmpty()) {
                field.set(entity, new ArrayList<>());
                return;
            }

            List<?> targetEntities = findAllByIdWithoutRelationships(targetRepository, targetEntityClass, targetIds);
            List<Object> relatedEntities = new ArrayList<>();

            for (Object targetEntity : targetEntities) {
                if (!mappedBy.isEmpty()) {
                    try {
                        Field mappedByField = targetEntityClass.getDeclaredField(mappedBy);
                        mappedByField.setAccessible(true);
                        Object mappedByValue = mappedByField.get(targetEntity);

                        if (mappedByValue != null) {
                            Field mappedByIdField = SQLGenerator.getIdField(entityClass);
                            mappedByIdField.setAccessible(true);
                            Object mappedById = mappedByIdField.get(mappedByValue);

                            if (entityId.equals(mappedById)) {
                                relatedEntities.add(targetEntity);
                            }
                        }
                    } catch (NoSuchFieldException e) {
                        throw new RuntimeException("Mapped field not found: " + mappedBy, e);
                    }
                }
            }

            field.set(entity, relatedEntities);
        } catch (NoSuchMethodException | InstantiationException | InvocationTargetException e) {
            throw new RuntimeException("Error loading one-to-many relationship", e);
        } finally {
            PROCESSED_ENTITIES.get().remove(entity);
        }
    }

    private void loadManyToOneRelationship(T entity, Field field) throws IllegalAccessException {
        if (PROCESSED_ENTITIES.get().contains(entity)) {
            return;
        }

        PROCESSED_ENTITIES.get().add(entity);

        try {
            ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
            Class<?> targetEntityClass = manyToOne.entity();
            String mappedBy = manyToOne.mappedBy();

            Field entityIdField = SQLGenerator.getIdField(entityClass);
            entityIdField.setAccessible(true);
            Object entityId = entityIdField.get(entity);

            Constructor<?> constructor = CrudRepositoryImpl.class.getConstructor(Class.class);
            Object targetRepository = constructor.newInstance(targetEntityClass);

            Entity targetEntityAnnotation = targetEntityClass.getAnnotation(Entity.class);
            SQLNamesHelper targetSqlNamesHelper = new SQLNamesHelper(targetEntityClass.getName());
            String targetTableName = targetSqlNamesHelper.getTableName(targetEntityClass, targetEntityAnnotation.name());

            Connection connection = databaseConnector.getConnection();
            List<Object> targetIds = new ArrayList<>();
            String allIdsSQL = "SELECT id FROM " + targetTableName;
            try (PreparedStatement statement = connection.prepareStatement(allIdsSQL)) {
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    targetIds.add(resultSet.getObject(1));
                }
            } catch (SQLException e) {
                throw new RuntimeException("Error querying target entities", e);
            }

            if (targetIds.isEmpty()) {
                return;
            }

            List<?> targetEntities = findAllByIdWithoutRelationships(targetRepository, targetEntityClass, targetIds);

            for (Object targetEntity : targetEntities) {
                if (!mappedBy.isEmpty()) {
                    try {
                        Field mappedByField = targetEntityClass.getDeclaredField(mappedBy);
                        mappedByField.setAccessible(true);

                        if (Collection.class.isAssignableFrom(mappedByField.getType())) {
                            Entity entityAnnotation = entityClass.getAnnotation(Entity.class);
                            SQLNamesHelper sqlNamesHelper = new SQLNamesHelper(entityClass.getName());
                            String entityTableName = sqlNamesHelper.getTableName(entityClass, entityAnnotation.name());

                            Field targetIdField = SQLGenerator.getIdField(targetEntityClass);
                            targetIdField.setAccessible(true);
                            Object targetId = targetIdField.get(targetEntity);

                            String checkSQL = "";

                            if (mappedByField.isAnnotationPresent(ManyToMany.class)) {
                                List<String> tableNames = new ArrayList<>();
                                tableNames.add(entityTableName);
                                tableNames.add(targetTableName);
                                Collections.sort(tableNames);
                                String junctionTableName = tableNames.get(0) + "_" + tableNames.get(1);

                                checkSQL = "SELECT 1 FROM " + junctionTableName + 
                                          " WHERE " + entityTableName.toLowerCase() + "_id = ? AND " + 
                                          targetTableName.toLowerCase() + "_id = ?";

                                try (PreparedStatement checkStatement = connection.prepareStatement(checkSQL)) {
                                    checkStatement.setObject(1, entityId);
                                    checkStatement.setObject(2, targetId);
                                    ResultSet checkResult = checkStatement.executeQuery();

                                    if (checkResult.next()) {
                                        field.set(entity, targetEntity);
                                        break;
                                    }
                                } catch (SQLException e) {
                                    log.error("Cannot find the table with many to one relationship");
                                }
                            } else {
                                // For OneToMany, check if the target entity references this entity
                                checkSQL = "SELECT 1 FROM " + entityTableName +
                                          " WHERE id = ? AND " + targetTableName.toLowerCase() + "_id = ?";

                                try (PreparedStatement checkStatement = connection.prepareStatement(checkSQL)) {
                                    checkStatement.setObject(1, entityId);
                                    checkStatement.setObject(2, targetId);
                                    ResultSet checkResult = checkStatement.executeQuery();

                                    if (checkResult.next()) {
                                        field.set(entity, targetEntity);
                                        break;
                                    }
                                } catch (SQLException e) {
                                    // If the query fails, it might be because the column doesn't exist yet
                                }
                            }
                        }
                    } catch (NoSuchFieldException e) {
                        throw new RuntimeException("Mapped field not found: " + mappedBy, e);
                    }
                }
            }
        } catch (NoSuchMethodException | InstantiationException | InvocationTargetException e) {
            throw new RuntimeException("Error loading many-to-one relationship", e);
        } finally {
            PROCESSED_ENTITIES.get().remove(entity);
        }
    }

    private void loadManyToManyRelationship(T entity, Field field) throws IllegalAccessException {
        if (PROCESSED_ENTITIES.get().contains(entity)) {
            return;
        }

        PROCESSED_ENTITIES.get().add(entity);

        try {
            ManyToMany manyToMany = field.getAnnotation(ManyToMany.class);
            Class<?> targetEntityClass = manyToMany.entity();

            Field entityIdField = SQLGenerator.getIdField(entityClass);
            entityIdField.setAccessible(true);
            Object entityId = entityIdField.get(entity);

            Constructor<?> constructor = CrudRepositoryImpl.class.getConstructor(Class.class);
            Object targetRepository = constructor.newInstance(targetEntityClass);

            Entity entityAnnotation = entityClass.getAnnotation(Entity.class);
            Entity targetEntityAnnotation = targetEntityClass.getAnnotation(Entity.class);

            SQLNamesHelper sqlNamesHelper = new SQLNamesHelper(entityClass.getName());
            SQLNamesHelper targetSqlNamesHelper = new SQLNamesHelper(targetEntityClass.getName());

            String entityTableName = sqlNamesHelper.getTableName(entityClass, entityAnnotation.name());
            String targetTableName = targetSqlNamesHelper.getTableName(targetEntityClass, targetEntityAnnotation.name());

            List<String> tableNames = new ArrayList<>();
            tableNames.add(entityTableName);
            tableNames.add(targetTableName);
            Collections.sort(tableNames);

            String junctionTableName = tableNames.get(0) + "_" + tableNames.get(1);

            Connection connection = databaseConnector.getConnection();
            String querySQL = "SELECT " + targetTableName.toLowerCase() + "_id FROM " + 
                             junctionTableName + " WHERE " + entityTableName.toLowerCase() + "_id = ?";

            List<Object> targetIds = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(querySQL)) {
                statement.setObject(1, entityId);
                ResultSet resultSet = statement.executeQuery();

                while (resultSet.next()) {
                    targetIds.add(resultSet.getObject(1));
                }
            } catch (SQLException e) {
                throw new RuntimeException("Error querying junction table", e);
            }

            if (targetIds.isEmpty()) {
                field.set(entity, new ArrayList<>());
                return;
            }

            List<?> relatedEntities = findAllByIdWithoutRelationships(targetRepository, targetEntityClass, targetIds);

            field.set(entity, relatedEntities);
        } catch (NoSuchMethodException | InstantiationException | InvocationTargetException e) {
            throw new RuntimeException("Error loading many-to-many relationship", e);
        } finally {
            PROCESSED_ENTITIES.get().remove(entity);
        }
    }

    private Optional<T> findByIdWithoutRelationships(ID id) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null");
        }

        Connection connection = databaseConnector.getConnection();
        String sql = SQLGenerator.generateSelectByIdSQL(tableName, fields, idField);

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                T entity = createEntityFromResultSet(resultSet);
                return Optional.of(entity);
            } else {
                return Optional.empty();
            }
        } catch (SQLException | ReflectiveOperationException e) {
            throw new RuntimeException("Error finding entity by ID", e);
        }
    }

    private List<?> findAllByIdWithoutRelationships(Object repository, Class<?> entityClass, List<Object> ids)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        List<Object> result = new ArrayList<>();

        java.lang.reflect.Method findByIdMethod = CrudRepositoryImpl.class.getDeclaredMethod("findByIdWithoutRelationships", Object.class);
        findByIdMethod.setAccessible(true);

        for (Object id : ids) {
            Optional<?> optionalEntity = (Optional<?>) findByIdMethod.invoke(repository, id);

            if (optionalEntity.isPresent()) {
                Object entity = optionalEntity.get();
                result.add(entity);
            }
        }

        return result;
    }
}
