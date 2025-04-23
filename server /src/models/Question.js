import { DataTypes } from 'sequelize';

export default function (sequelize) {
    const Question = sequelize.define('Question', {
        question: {
            type: DataTypes.TEXT,
            allowNull: false,
        },
        alternatives: {
            type: DataTypes.JSON,
            allowNull: false,
            validate: {
                isArray(value) {
                    if (!Array.isArray(value)) {
                        throw new Error('Alternativas devem ser um array');
                    }
                },
            },
        },
    });

    Question.associate = function (models) {
        Question.hasMany(models.Survey, {
            foreignKey: 'questionId',
            as: 'surveys',
        });
    };

    return Question;
}