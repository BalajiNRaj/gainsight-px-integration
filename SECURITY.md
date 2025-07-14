# Security Guidelines

## MongoDB Credentials Management

### Important Security Notes

⚠️ **Never commit actual MongoDB credentials to version control!**

All MongoDB connection strings in this repository use placeholder environment variables (`${MONGODB_USERNAME}` and `${MONGODB_PASSWORD}`) for security purposes.

### Setting Up Environment Variables

#### For Local Development
Create a `.env` file in your project root (this file should be in `.gitignore`):
```bash
MONGODB_USERNAME=your_actual_username
MONGODB_PASSWORD=your_actual_password
```

#### For Production Deployment
Set environment variables in your deployment environment:
```bash
export MONGODB_USERNAME=your_actual_username
export MONGODB_PASSWORD=your_actual_password
```

#### For Spring Boot Applications
You can also use `application-local.properties` (excluded from version control):
```properties
spring.data.mongodb.uri=mongodb+srv://your_actual_username:your_actual_password@cluster.mongodb.net/gainsightdb?retryWrites=true&w=majority
```

### Best Practices

1. **Use Environment Variables**: Always use environment variables or external configuration for sensitive data
2. **Rotate Credentials Regularly**: Change MongoDB passwords periodically
3. **Use Strong Passwords**: Generate complex, unique passwords for database access
4. **Limit Access**: Grant minimal required permissions to database users
5. **Network Security**: Use IP whitelisting and VPN access where possible
6. **Monitor Access**: Enable database audit logging and monitor for unusual activity

### Credential Rotation

If you suspect credentials have been compromised:

1. **Immediately rotate** MongoDB Atlas user passwords
2. **Update** all environment variables in deployment environments
3. **Restart** application instances to pick up new credentials
4. **Audit** database access logs for any suspicious activity
5. **Review** and update IP whitelist if necessary

### Files That Should Never Contain Real Credentials

- README.md
- DEPLOYMENT_GUIDE.md
- USAGE_GUIDE.md
- QUICK_REFERENCE.md
- scripts/README.md
- Any file committed to version control

### Emergency Response

If credentials are accidentally committed:

1. **Immediately** change the MongoDB password
2. **Force push** to remove credentials from git history (if safe to do so)
3. **Audit** recent database access
4. **Review** repository access permissions
5. **Update** all deployment environments with new credentials
