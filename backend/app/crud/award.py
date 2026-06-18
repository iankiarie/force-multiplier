from sqlalchemy.orm import Session
from app import models, schemas


def get_award(db: Session, award_id: int):
    return db.query(models.Award).filter(models.Award.id == award_id).first()


def get_awards(db: Session, skip: int = 0, limit: int = 100):
    return db.query(models.Award).offset(skip).limit(limit).all()


def get_awards_by_user(db: Session, user_id: int, skip: int = 0, limit: int = 100):
    return db.query(models.Award).filter(models.Award.user_id == user_id).offset(skip).limit(limit).all()


def get_awards_by_month(db: Session, month: str, skip: int = 0, limit: int = 100):
    # month format: YYYY-MM
    return db.query(models.Award).filter(models.Award.month == month).offset(skip).limit(limit).all()


def create_award(db: Session, award_in: schemas.AwardCreate, user_id: int):
    db_award = models.Award(
        score=award_in.score,
        month=award_in.month,
        user_id=user_id,
    )
    db.add(db_award)
    db.commit()
    db.refresh(db_award)
    return db_award


def update_award(db: Session, db_award: models.Award, award_in: schemas.AwardUpdate):
    update_data = award_in.dict(exclude_unset=True)
    for field, value in update_data.items():
        setattr(db_award, field, value)
    db.add(db_award)
    db.commit()
    db.refresh(db_award)
    return db_award


def delete_award(db: Session, award_id: int):
    db_award = db.query(award.Award).filter(award.Award.id == award_id).first()
    if db_award:
        db.delete(db_award)
        db.commit()
    return db_award