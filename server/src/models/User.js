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
    surname: { type: DataTypes.STRING },
    genero: { type: DataTypes.STRING },
    tipoConta: { type: DataTypes.STRING },
    cpf: { type: DataTypes.STRING },
    cnh: { type: DataTypes.STRING },
    carro: { type: DataTypes.JSON },
    salt: { type: DataTypes.STRING },
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

  User.beforeCreate(async (user) => {
    const hashedPassword = await bcrypt.hash(user.password, 10);
    user.password = hashedPassword;
  });

  return User;
}