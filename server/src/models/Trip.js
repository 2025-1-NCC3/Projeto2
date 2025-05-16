import { DataTypes } from 'sequelize';

export default function (sequelize) {
    const Trip = sequelize.define('Trip', {
        origem: { type: DataTypes.GEOMETRY('POINT'), allowNull: false },
        destino: { type: DataTypes.GEOMETRY('POINT'), allowNull: false },
        status: {
            type: DataTypes.ENUM,
            values: ['pendente', 'aceita', 'em_andamento', 'concluida', 'cancelada'],
            defaultValue: 'pendente'
        },
        preco: { type: DataTypes.FLOAT }
    });

    Trip.associate = function (models) {
        Trip.belongsTo(models.User, { as: 'Motorista', foreignKey: 'driverId' });
        Trip.belongsTo(models.User, { as: 'Passageiro', foreignKey: 'passengerId' });
        Trip.belongsTo(models.Car, { foreignKey: 'carId' });
    };

    return Trip;
}