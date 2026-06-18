from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from typing import List

from app import crud, schemas
from app.deps import get_db, get_current_active_user

router = APIRouter()


@router.post("/", response_model=schemas.Transaction, status_code=status.HTTP_201_CREATED)
def create_transaction(
    transaction_in: schemas.TransactionCreate,
    db: Session = Depends(get_db),
    current_user: schemas.User = Depends(get_current_active_user)
):
    # Only allow creating transactions for the current user (or admin for others? We'll keep it simple: users can only create their own)
    if transaction_in.user_id != current_user.id:
        raise HTTPException(status_code=403, detail="Not enough permissions")
    return crud.create_transaction(
        db=db,
        amount=transaction_in.amount,
        type_=transaction_in.type.value,
        description=transaction_in.description,
        user_id=transaction_in.user_id
    )


@router.get("/", response_model=List[schemas.Transaction])
def read_transactions(
    skip: int = 0,
    limit: int = 100,
    db: Session = Depends(get_db),
    current_user: schemas.User = Depends(get_current_active_user)
):
    transactions = crud.get_transactions_by_user(db, user_id=current_user.id, skip=skip, limit=limit)
    return transactions


@router.get("/{transaction_id}", response_model=schemas.Transaction)
def read_transaction(
    transaction_id: int,
    db: Session = Depends(get_db),
    current_user: schemas.User = Depends(get_current_active_user)
):
    transaction = crud.get_transaction(db, transaction_id=transaction_id)
    if not transaction:
        raise HTTPException(status_code=404, detail="Transaction not found")
    # Check ownership
    if transaction.user_id != current_user.id:
        raise HTTPException(status_code=403, detail="Not enough permissions")
    return transaction


# We don't have update/delete for transactions in the spec, but we can leave them out for now.