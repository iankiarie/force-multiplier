from sqlalchemy.orm import Session
from app.models import transaction
from app.schemas import transaction as transaction_schema


def get_transaction(db: Session, transaction_id: int):
    return db.query(transaction.Transaction).filter(transaction.Transaction.id == transaction_id).first()


def get_transactions_by_user(db: Session, user_id: int, skip: int = 0, limit: int = 100):
    return db.query(transaction.Transaction).filter(transaction.Transaction.user_id == user_id).offset(skip).limit(limit).all()


def create_transaction(db: Session, amount: int, type_: str, description: str | None, user_id: int):
    db_transaction = transaction.Transaction(
        amount=amount,
        type=type_,
        description=description,
        user_id=user_id,
    )
    db.add(db_transaction)
    db.commit()
    db.refresh(db_transaction)
    return db_transaction


def update_transaction(db: Session, db_transaction: transaction.Transaction, transaction_in: transaction_schema.TransactionUpdate):
    update_data = transaction_in.dict(exclude_unset=True)
    for field, value in update_data.items():
        setattr(db_transaction, field, value)
    db.add(db_transaction)
    db.commit()
    db.refresh(db_transaction)
    return db_transaction


def delete_transaction(db: Session, transaction_id: int):
    db_transaction = db.query(transaction.Transaction).filter(transaction.Transaction.id == transaction_id).first()
    if db_transaction:
        db.delete(db_transaction)
        db.commit()
    return db_transaction