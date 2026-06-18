from pydantic import BaseModel, Field
from typing import Optional, List
from datetime import datetime


class NoteBase(BaseModel):
    title: str = Field(..., min_length=1, max_length=200)
    content: str
    tags: Optional[List[str]] = None


class NoteCreate(NoteBase):
    user_id: int


class NoteUpdate(BaseModel):
    title: Optional[str] = Field(None, min_length=1, max_length=200)
    content: Optional[str] = None
    tags: Optional[List[str]] = None


class NoteInDBBase(NoteBase):
    id: int
    user_id: int
    created_at: datetime
    updated_at: Optional[datetime] = None

    class Config:
        from_attributes = True


class Note(NoteInDBBase):
    pass
