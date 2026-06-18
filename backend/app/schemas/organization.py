"""
Organization schema.
"""
from pydantic import BaseModel
from typing import Optional


class OrganizationBase(BaseModel):
    name: str
    description: Optional[str] = None
    is_active: Optional[bool] = True


class OrganizationCreate(OrganizationBase):
    pass


class OrganizationUpdate(BaseModel):
    name: Optional[str] = None
    description: Optional[str] = None
    is_active: Optional[bool] = None


class OrganizationInDBBase(OrganizationBase):
    id: int

    class Config:
        orm_mode = True


class Organization(OrganizationInDBBase):
    pass