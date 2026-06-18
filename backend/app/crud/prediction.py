from sqlalchemy.orm import Session
from app.models import prediction
from app.schemas import prediction as prediction_schema


def get_prediction(db: Session, prediction_id: int):
    return db.query(prediction.Prediction).filter(prediction.Prediction.id == prediction_id).first()


def get_predictions(db: Session, skip: int = 0, limit: int = 100):
    return db.query(prediction.Prediction).offset(skip).limit(limit).all()


def get_predictions_by_creator(db: Session, creator_id: int, skip: int = 0, limit: int = 100):
    return db.query(prediction.Prediction).filter(prediction.Prediction.created_by == creator_id).offset(skip).limit(limit).all()


def create_prediction(db: Session, prediction_in: prediction_schema.PredictionCreate, creator_id: int):
    db_prediction = prediction.Prediction(
        title=prediction_in.title,
        description=prediction_in.description,
        deadline=prediction_in.deadline,
        created_by=creator_id,
    )
    db.add(db_prediction)
    db.commit()
    db.refresh(db_prediction)
    return db_prediction


def update_prediction(db: Session, db_prediction: prediction.Prediction, prediction_in: prediction_schema.PredictionUpdate):
    update_data = prediction_in.dict(exclude_unset=True)
    for field, value in update_data.items():
        setattr(db_prediction, field, value)
    db.add(db_prediction)
    db.commit()
    db.refresh(db_prediction)
    return db_prediction


def delete_prediction(db: Session, prediction_id: int):
    db_prediction = db.query(prediction.Prediction).filter(prediction.Prediction.id == prediction_id).first()
    if db_prediction:
        db.delete(db_prediction)
        db.commit()
    return db_prediction