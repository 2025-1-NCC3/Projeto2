import { DataTypes } from 'sequelize';

export default (sequelize) => {
  const Survey = sequelize.define('Survey', {
    id: { type: DataTypes.INTEGER, primaryKey: true, autoIncrement: true },
    userId: { type: DataTypes.INTEGER, allowNull: false },
    questionId: { type: DataTypes.INTEGER, allowNull: false },
    answer: { type: DataTypes.ENUM('sim', 'não'), allowNull: false }
  });
  // sem associação com Question
  Survey.removeAttribute('updatedAt');
  Survey.removeAttribute('createdAt');
  return Survey;
};