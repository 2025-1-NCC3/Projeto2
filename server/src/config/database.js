import { Sequelize } from 'sequelize';

const sequelize = new Sequelize("db-system", "root", "password", {
  dialect: 'sqlite',
  storage: './dev.sqlite',
  logging: false
});

export default sequelize;