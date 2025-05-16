import sequelize from '../config/database.js';
import User from './User.js';
import Survey from './Survey.js';

const models = {
  User: User(sequelize),
  Survey: Survey(sequelize),
};

Object.values(models).forEach((model) => {
  if (model.associate) {
    model.associate(models);
  }
});

export default models;