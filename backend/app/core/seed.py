"""
Seed script to create initial data for the ForceMultiplier app.
Run with: docker compose exec api python -m app.core.seed
"""
from sqlalchemy.orm import Session
from app.database import SessionLocal, engine
from app.models import user as user_model
from app.models import organization as org_model
from app.models import role as role_model
from app.models import user_organization as uo_model
from app.core.security import get_password_hash

# Create tables
user_model.Base.metadata.create_all(bind=engine)
org_model.Base.metadata.create_all(bind=engine)
role_model.Base.metadata.create_all(bind=engine)
uo_model.Base.metadata.create_all(bind=engine)

def init_db(db: Session) -> None:
    # Create organizations if not exist
    demo_org = db.query(org_model.Organization).filter(org_model.Organization.name == "Demo Company").first()
    if not demo_org:
        demo_org = org_model.Organization(
            name="Demo Company",
            description="Demo organization for ForceMultiplier",
            is_active=True,
        )
        db.add(demo_org)
        db.commit()
        db.refresh(demo_org)
        print("Created organization: Demo Company")
    else:
        print("Organization Demo Company already exists")

    ketha_org = db.query(org_model.Organization).filter(org_model.Organization.name == "ketha").first()
    if not ketha_org:
        ketha_org = org_model.Organization(
            name="ketha",
            description="Ketha Africa organization",
            is_active=True,
        )
        db.add(ketha_org)
        db.commit()
        db.refresh(ketha_org)
        print("Created organization: ketha")
    else:
        print("Organization ketha already exists")

    # Create roles if not exist
    admin_role = db.query(role_model.Role).filter(role_model.Role.name == "admin").first()
    if not admin_role:
        admin_role = role_model.Role(
            name="admin",
            description="Administrator role",
            is_active=True,
        )
        db.add(admin_role)
        db.commit()
        db.refresh(admin_role)
        print("Created role: admin")
    else:
        print("Role admin already exists")

    user_role = db.query(role_model.Role).filter(role_model.Role.name == "user").first()
    if not user_role:
        user_role = role_model.Role(
            name="user",
            description="Regular user role",
            is_active=True,
        )
        db.add(user_role)
        db.commit()
        db.refresh(user_role)
        print("Created role: user")
    else:
        print("Role user already exists")

    # Demo Company: Create admin user if not exists
    admin = db.query(user_model.User).filter(user_model.User.email == "admin@forcemultiplier.com").first()
    if not admin:
        admin_user = user_model.User(
            email="admin@forcemultiplier.com",
            username="admin",
            full_name="Admin User",
            hashed_password=get_password_hash("admin"),
            is_active=True,
            points=0,  # Starting points for admin
        )
        db.add(admin_user)
        db.commit()
        db.refresh(admin_user)
        print("Created admin user for Demo Company")
        # Assign admin user to Demo Company organization with admin role
        uo = uo_model.UserOrganization(
            user_id=admin_user.id,
            organization_id=demo_org.id,
            role_id=admin_role.id,
            is_active=True,
        )
        db.add(uo)
        db.commit()
        print("Assigned admin user to Demo Company")
    else:
        print("Admin user for Demo Company already exists")
        # Ensure admin user has the admin role in Demo Company (if not, create)
        existing_uo = db.query(uo_model.UserOrganization).filter(
            uo_model.UserOrganization.user_id == admin.id,
            uo_model.UserOrganization.organization_id == demo_org.id,
            uo_model.UserOrganization.role_id == admin_role.id
        ).first()
        if not existing_uo:
            uo = uo_model.UserOrganization(
                user_id=admin.id,
                organization_id=demo_org.id,
                role_id=admin_role.id,
                is_active=True,
            )
            db.add(uo)
            db.commit()
            print("Assigned admin user to Demo Company (was missing)")

    # Demo Company: Create demo regular user if not exists
    demo_user = db.query(user_model.User).filter(user_model.User.email == "user@example.com").first()
    if not demo_user:
        regular_user = user_model.User(
            email="user@example.com",
            username="demouser",
            full_name="Demo User",
            hashed_password=get_password_hash("password"),
            is_active=True,
            points=1000,  # Starting points for demo user
        )
        db.add(regular_user)
        db.commit()
        db.refresh(regular_user)
        print("Created demo user for Demo Company")
        # Assign demo user to Demo Company organization with user role
        uo = uo_model.UserOrganization(
            user_id=regular_user.id,
            organization_id=demo_org.id,
            role_id=user_role.id,
            is_active=True,
        )
        db.add(uo)
        db.commit()
        print("Assigned demo user to Demo Company")
    else:
        print("Demo user for Demo Company already exists")
        # Ensure demo user has the user role in Demo Company (if not, create)
        existing_uo = db.query(uo_model.UserOrganization).filter(
            uo_model.UserOrganization.user_id == demo_user.id,
            uo_model.UserOrganization.organization_id == demo_org.id,
            uo_model.UserOrganization.role_id == user_role.id
        ).first()
        if not existing_uo:
            uo = uo_model.UserOrganization(
                user_id=demo_user.id,
                organization_id=demo_org.id,
                role_id=user_role.id,
                is_active=True,
            )
            db.add(uo)
            db.commit()
            print("Assigned demo user to Demo Company (was missing)")

    # ketha organization: Create admin user if not exists
    ketha_user = db.query(user_model.User).filter(user_model.User.email == "iankiarie@ketha.africa").first()
    if not ketha_user:
        ketha_admin_user = user_model.User(
            email="iankiarie@ketha.africa",
            username="iankiarie",
            full_name="Ian Kiarie",
            hashed_password=get_password_hash("ketha123"),  # You should change this in production
            is_active=True,
            points=0,  # Starting points
        )
        db.add(ketha_admin_user)
        db.commit()
        db.refresh(ketha_admin_user)
        print("Created admin user for ketha organization")
        # Assign ketha admin user to ketha organization with admin role
        uo = uo_model.UserOrganization(
            user_id=ketha_admin_user.id,
            organization_id=ketha_org.id,
            role_id=admin_role.id,
            is_active=True,
        )
        db.add(uo)
        db.commit()
        print("Assigned admin user to ketha organization")
    else:
        print("Admin user for ketha organization already exists")
        # Ensure ketha user has the admin role in ketha organization (if not, create)
        existing_uo = db.query(uo_model.UserOrganization).filter(
            uo_model.UserOrganization.user_id == ketha_user.id,
            uo_model.UserOrganization.organization_id == ketha_org.id,
            uo_model.UserOrganization.role_id == admin_role.id
        ).first()
        if not existing_uo:
            uo = uo_model.UserOrganization(
                user_id=ketha_user.id,
                organization_id=ketha_org.id,
                role_id=admin_role.id,
                is_active=True,
            )
            db.add(uo)
            db.commit()
            print("Assigned admin user to ketha organization (was missing)")

if __name__ == "__main__":
    db = SessionLocal()
    try:
        init_db(db)
    finally:
        db.close()