from pydantic import BaseModel
from typing import Optional


class UserOrganizationBase(BaseModel):
    user_id: int
    organization_id: int
    role_id: int
    is_active: Optional[bool] = True


class UserOrganizationCreate(UserOrganizationBase):
    pass


class UserOrganizationUpdate(BaseModel):
    user_id: Optional[int] = None
    organization_id: Optional[int] = None
    role_id: Optional[int] = None
    is_active: Optional[bool] = None


class UserOrganizationInDBBase(UserOrganizationBase):
    id: int

    class Config:
        from_attributes = True


class UserOrganization(UserOrganizationInDBBase):
    pass
