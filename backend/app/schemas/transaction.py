from pydantic import BaseModel
from typing import Optional
from datetime import datetime
from app.models.transaction import TransactionType


class TransactionBase(BaseModel):
    amount: int
    type: TransactionType
    description: Optional[str] = None


class TransactionCreate(TransactionBase):
    user_id: int


class TransactionUpdate(BaseModel):
    amount: Optional[int] = None
    type: Optional[TransactionType] = None
    description: Optional[str] = None


class TransactionInDBBase(TransactionBase):
    id: int
    created_at: datetime
    user_id: int

    class Config:
        from_attributes = True


class Transaction(TransactionInDBBase):
    pass
