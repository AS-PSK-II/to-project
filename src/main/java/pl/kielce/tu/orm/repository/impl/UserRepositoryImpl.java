package pl.kielce.tu.orm.repository.impl;

import pl.kielce.tu.orm.annotations.Entity;
import pl.kielce.tu.orm.sql.SQLNamesHelper;
import pl.kielce.tu.orm.connector.DatabaseConnector;
import pl.kielce.tu.orm.entities.User;
import pl.kielce.tu.orm.repository.UserRepository;
import pl.kielce.tu.orm.sql.SQLGenerator;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserRepositoryImpl extends CrudRepositoryImpl<User, Long> implements UserRepository {

    public UserRepositoryImpl() {
        super(User.class);
    }

    public UserRepositoryImpl(Class<User> entityClass) {
        super(entityClass);
    }

    @Override
    public List<User> findByName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Name must not be null");
        }

        Entity entityAnnotation = User.class.getAnnotation(Entity.class);

        Connection connection = DatabaseConnector.getInstance().getConnection();
        String tableName = new SQLNamesHelper(User.class.getName()).getTableName(User.class, entityAnnotation.name());

        Field nameField = null;
        for (Field field : User.class.getDeclaredFields()) {
            if (field.getName().equals("name")) {
                nameField = field;
                break;
            }
        }

        if (nameField == null) {
            throw new IllegalStateException("Name field not found in User class");
        }

        String sql = SQLGenerator.generateSelectSQL(tableName, SQLGenerator.getFields(User.class)) +
                " WHERE " + new SQLNamesHelper(User.class.getName()).getColumnName(nameField, "") + " = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);

            ResultSet resultSet = statement.executeQuery();

            List<User> result = new ArrayList<>();
            while (resultSet.next()) {
                User user = createEntityFromResultSet(resultSet);
                result.add(user);
            }

            return result;
        } catch (SQLException | ReflectiveOperationException e) {
            throw new RuntimeException("Error finding users by name", e);
        }
    }

    private User createEntityFromResultSet(ResultSet resultSet) throws SQLException, ReflectiveOperationException {
        User user = new User();

        for (Field field : User.class.getDeclaredFields()) {
            field.setAccessible(true);
            SQLNamesHelper sqlNamesHelper = new SQLNamesHelper(User.class.getName());
            String columnName = sqlNamesHelper.getColumnName(field, "");
            Object value = resultSet.getObject(columnName);

            if (value != null) {
                field.set(user, value);
            }
        }

        return user;
    }
}
