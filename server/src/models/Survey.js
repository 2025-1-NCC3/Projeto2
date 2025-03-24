import { DataTypes } from 'sequelize';

export default function (sequelize) {
  const Survey = sequelize.define('Survey', {
    questionId: {
      type: DataTypes.INTEGER,
      allowNull: false,
      references: {
        model: 'Questions',
        key: 'id',
      },
    },
    answer: {
      type: DataTypes.TEXT,
      allowNull: false,
    },
    userId: {
      type: DataTypes.INTEGER,
      allowNull: false,
      references: {
        model: 'Users',
        key: 'id',
      },
    },
  });

  Survey.associate = function (models) {
    Survey.belongsTo(models.User, {
      foreignKey: 'userId',
      as: 'user',
    });

    Survey.belongsTo(models.Question, {
      foreignKey: 'questionId',
      as: 'question',
    });
  };

  return Survey;
}