"""
Role schema.
"""
from pydantic import BaseModel
from typing import Optional


class RoleBase(BaseModel):
    name: str
    description: Optional[str] = None
    is_active: Optional[bool] = True


class RoleCreate(RoleBase):
    pass


class RoleUpdate(BaseModel):
    name: Optional[str] = None
    description: Optional[str] = None
    is_active: Optional[bool] = None


class RoleInDBBase(RoleBase):
    id: int

    class Config:
        orm_mode = True


class Role(RoleInDBBase):
    pass