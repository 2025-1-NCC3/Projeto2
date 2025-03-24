import { DataTypes } from 'sequelize';
import bcrypt from 'bcrypt';

export default function (sequelize) {
  const User = sequelize.define('User', {
    name: {
      type: DataTypes.STRING,
      allowNull: false,
    },
    email: {
      type: DataTypes.STRING,
      allowNull: false,
      unique: true,
      validate: {
        isEmail: true,
      },
    },
    phone: {
      type: DataTypes.STRING,
      allowNull: false,
    },
    birthday: {
      type: DataTypes.DATE,
      allowNull: false,
    },
    password: {
      type: DataTypes.STRING,
      allowNull: false,
      validate: {
        len: [8, 100],
      },
    },
  }, {
    tableName: 'users',
    timestamps: true,
  });

  User.associate = function (models) {
    User.hasMany(models.Survey, {
      foreignKey: 'userId',
      as: 'surveys',
    });
  };

  User.beforeCreate(async (user) => {
    const hashedPassword = await bcrypt.hash(user.password, 10);
    user.password = hashedPassword;
  });

  return User;
}