"""
CRUD operations for Role model.
"""
from sqlalchemy.orm import Session

from app.models import role
from app.schemas import role as role_schema


def get_role(db: Session, role_id: int):
    return db.query(role.Role).filter(role.Role.id == role_id).first()


def get_role_by_name(db: Session, name: str):
    return db.query(role.Role).filter(role.Role.name == name).first()


def get_roles(db: Session, skip: int = 0, limit: int = 100):
    return db.query(role.Role).offset(skip).limit(limit).all()


def create_role(db: Session, role_in: role_schema.RoleCreate):
    db_role = role.Role(
        name=role_in.name,
        description=role_in.description,
        is_active=role_in.is_active,
    )
    db.add(db_role)
    db.commit()
    db.refresh(db_role)
    return db_role


def update_role(db: Session, db_role: role.Role, role_in: role_schema.RoleUpdate):
    update_data = role_in.dict(exclude_unset=True)
    for field, value in update_data.items():
        setattr(db_role, field, value)
    db.add(db_role)
    db.commit()
    db.refresh(db_role)
    return db_role


def delete_role(db: Session, role_id: int):
    db_role = db.query(role.Role).filter(role.Role.id == role_id).first()
    if db_role:
        db.delete(db_role)
        db.commit()
    return db_role