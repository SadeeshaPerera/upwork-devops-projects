# MySQL 5.x and Tomcat 8/9 Setup on Ubuntu Server

**Project scope:** Performed a clean installation, configuration, security hardening, and prepared ongoing support guidance for MySQL 5.x and Tomcat 8 or 9 on Ubuntu Server.

> **Note:** This documentation contained no client-sensitive data. All examples used placeholders for credentials and domain names.

---

## Table of Contents
1. [Environment Overview](#environment-overview)
2. [Prerequisites](#prerequisites)
3. [MySQL 5.x Installation](#mysql-5x-installation)
4. [Tomcat Installation](#tomcat-installation)
5. [Security Hardening](#security-hardening)
6. [Verification & Testing](#verification--testing)
7. [Ongoing Maintenance](#ongoing-maintenance)
8. [Troubleshooting](#troubleshooting)

---

## Environment Overview

- **OS:** Ubuntu 22.04 LTS
- **MySQL Version:** 5.7
- **Tomcat Version:** 9.0
- **Java Version:** OpenJDK 11
- **User Context:** Non-root user with sudo privileges
- **Firewall:** UFW (Uncomplicated Firewall)

---

## Prerequisites

### System Update
```bash
sudo apt update && sudo apt upgrade -y
```

### Created Service User
```bash
sudo useradd -m -d /opt/tomcat -U -s /bin/false tomcat
```

### Installed Essential Tools
```bash
sudo apt install -y wget curl vim ufw
```

---

## MySQL Installation

### Installed MySQL 5.7 (Ubuntu 22.04)

MySQL 5.7 was not included in the Ubuntu 22.04 default repositories. The recommended approach was to use the official MySQL APT repository to install MySQL 5.7 on Ubuntu 22.04. Alternatively, MySQL 5.7 could be run in a container if isolation was preferred.

```bash
# Download the MySQL APT config package from Oracle
wget https://dev.mysql.com/get/mysql-apt-config_0.8.24-1_all.deb

# Install the APT config package non-interactively. If interactive prompts appeared,
# run `sudo dpkg-reconfigure mysql-apt-config` and select MySQL 5.7.
sudo DEBIAN_FRONTEND=noninteractive dpkg -i mysql-apt-config_0.8.24-1_all.deb || true


sudo apt update
sudo apt install -y mysql-server
```

Notes:
- The APT config package version might change; check https://dev.mysql.com/downloads/repo/apt/ for the latest.

### Started and enabled MySQL
```bash
sudo systemctl start mysql
sudo systemctl enable mysql
sudo systemctl status mysql
```

### Secured MySQL Installation
```bash
sudo mysql_secure_installation
```

**Answered prompts as follows:**
- Set root password: **Yes**
- Removed anonymous users: **Yes**
- Disallowed root login remotely: **Yes**
- Removed test database: **Yes**
- Reloaded privilege tables: **Yes**

### Created Application Database and User
```bash
sudo mysql -u root -p
```

Inside MySQL shell:
```sql
CREATE DATABASE appdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'appuser'@'localhost' IDENTIFIED BY 'STRONG_PASSWORD';
GRANT ALL PRIVILEGES ON appdb.* TO 'appuser'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

### Verified MySQL Installation
```bash
mysql -u appuser -p -e "SHOW DATABASES;"
```

---

## Tomcat Installation

### Installed Java (Required for Tomcat)

**For Tomcat 9:**
```bash
sudo apt install -y openjdk-11-jdk
```

Verified Java:
```bash
java -version
```

### Downloaded and installed Tomcat

#### Tomcat 9 (recommended version)
```bash
cd /tmp
wget https://archive.apache.org/dist/tomcat/tomcat-9/v9.0.80/bin/apache-tomcat-9.0.80.tar.gz
sudo tar -xzf apache-tomcat-9.0.80.tar.gz -C /opt/tomcat --strip-components=1
```


### Set permissions
```bash
sudo chown -R tomcat:tomcat /opt/tomcat
sudo chmod -R u+x /opt/tomcat/bin
```

### Configured environment variables
```bash
sudo vim /etc/environment
```

Added:
```
JAVA_HOME="/usr/lib/jvm/java-11-openjdk-amd64"
CATALINA_HOME="/opt/tomcat"
```

Reloaded:
```bash
source /etc/environment
```

### Created systemd service for Tomcat
```bash
sudo vim /etc/systemd/system/tomcat.service
```

Added the following:
```ini
[Unit]
Description=Apache Tomcat Web Application Container
After=network.target

[Service]
Type=forking

User=tomcat
Group=tomcat

Environment="JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64"
Environment="CATALINA_PID=/opt/tomcat/temp/tomcat.pid"
Environment="CATALINA_HOME=/opt/tomcat"
Environment="CATALINA_BASE=/opt/tomcat"
Environment="CATALINA_OPTS=-Xms512M -Xmx1024M -server -XX:+UseParallelGC"

ExecStart=/opt/tomcat/bin/startup.sh
ExecStop=/opt/tomcat/bin/shutdown.sh

RestartSec=10
Restart=always

[Install]
WantedBy=multi-user.target
```

### Started and enabled Tomcat
```bash
sudo systemctl daemon-reload
sudo systemctl start tomcat
sudo systemctl enable tomcat
sudo systemctl status tomcat
```

### Verified Tomcat
```bash
curl http://localhost:8080
```
---

## Security Hardening

### MySQL Security

#### 1. Bind to localhost only (Because DB is local to app)
```bash
sudo vim /etc/mysql/mysql.conf.d/mysqld.cnf
```

Ensured:
```ini
bind-address = 127.0.0.1
```

Restarted MySQL:
```bash
sudo systemctl restart mysql
```

#### 2. Disabled remote root login (already done in `mysql_secure_installation`)

#### 3. Used strong passwords for all DB users

#### 4. To update MySQL regularly
```bash
sudo apt update && sudo apt upgrade mysql-server
```

#### 5. Enable binary logging for point-in-time recovery
```bash
sudo vim /etc/mysql/mysql.conf.d/mysqld.cnf
```

Added:
```ini
log_bin = /var/log/mysql/mysql-bin.log
expire_logs_days = 10
```

Restarted:
```bash
sudo systemctl restart mysql
```

### Tomcat Security

#### 1. Configure Tomcat Users for Manager Access
```bash
sudo vim /opt/tomcat/conf/tomcat-users.xml
```

Add (inside `<tomcat-users>`):
```xml
<role rolename="manager-gui"/>
<role rolename="admin-gui"/>
<user username="admin" password="STRONG_PASSWORD_HERE" roles="manager-gui,admin-gui"/>
```

#### 2. Restrict Manager App Access to Localhost
```bash
sudo vim /opt/tomcat/webapps/manager/META-INF/context.xml
```

Commented out the `Valve` or restrict to your IP:
```xml
<Context antiResourceLocking="false" privileged="true" >
  <Valve className="org.apache.catalina.valves.RemoteAddrValve"
         allow="127\.\d+\.\d+\.\d+|::1|0:0:0:0:0:0:0:1|IP_HERE" />
</Context>
```

Do the same for `/opt/tomcat/webapps/host-manager/META-INF/context.xml`.

#### 3. Disable Unnecessary Webapps
```bash
sudo rm -rf /opt/tomcat/webapps/docs /opt/tomcat/webapps/examples
```

#### 4. Run Tomcat as Non-Root (already configured via `tomcat` user)

#### 5. Set Proper File Permissions
```bash
sudo chown -R tomcat:tomcat /opt/tomcat
sudo chmod -R 750 /opt/tomcat/conf
sudo chmod 640 /opt/tomcat/conf/tomcat-users.xml
```

#### 6. Hide Tomcat Version
```bash
sudo vim /opt/tomcat/conf/server.xml
```

Found the `<Connector>` tag and added:
```xml
<Connector port="8080" protocol="HTTP/1.1"
           connectionTimeout="20000"
           redirectPort="8443"
           server="Apache" />
```

#### 7. Enable HTTPS
Used Let's Encrypt for production.


Restart Tomcat:
```bash
sudo systemctl restart tomcat
```

### Firewall Configuration (UFW)

```bash
sudo ufw allow OpenSSH
sudo ufw allow 8080/tcp    # Tomcat HTTP
sudo ufw allow 8443/tcp    # Tomcat HTTPS
sudo ufw enable
sudo ufw status
```

**Note:** MySQL port 3306 should NOT be exposed when DB is local.
```bash
sudo ufw allow from TRUSTED_IP to any port 3306
```

### Fail2Ban (Recommended for SSH brute-force protection)
```bash
sudo apt install -y fail2ban
sudo systemctl enable fail2ban
sudo systemctl start fail2ban
```

---

## Verification & Testing

### MySQL Verification
```bash
# Check service status
sudo systemctl status mysql

# Test login
mysql -u appuser -p -e "SELECT VERSION();"

# Check databases
mysql -u appuser -p -e "SHOW DATABASES;"
```

### Tomcat Verification
```bash
# Check service status
sudo systemctl status tomcat

# Test HTTP endpoint
curl http://localhost:8080

# Check logs
sudo tail -f /opt/tomcat/logs/catalina.out

# Test manager app (from browser/ curl)
curl -u admin:PASSWORD http://localhost:8080/manager/html
```

### Check Listening Ports
```bash
sudo netstat -tulpn | grep -E '3306|8080|8443'
```

Expected:
- MySQL on `127.0.0.1:3306` 
- Tomcat on `0.0.0.0:8080` and `0.0.0.0:8443`

---

## Ongoing Maintenance

### Performance Optimization

#### MySQL
- **Enable query cache** :
  ```ini
  query_cache_type = 1
  query_cache_size = 64M
  ```
- **Tune InnoDB buffer pool**:
  ```ini
  innodb_buffer_pool_size = 1G  # Based on RAM
  ```
- **Analyze slow queries**:
  ```ini
  slow_query_log = 1
  slow_query_log_file = /var/log/mysql/slow.log
  long_query_time = 2
  ```

#### Tomcat
- **Tune JVM heap** in `/etc/systemd/system/tomcat.service`:
  ```
  Environment="CATALINA_OPTS=-Xms1024M -Xmx2048M -server -XX:+UseG1GC"
  ```
- **Connection pool tuning** in `server.xml`:
  ```xml
  <Connector port="8080" maxThreads="200" minSpareThreads="25" />
  ```

### Monitoring & Troubleshooting

#### MySQL
```bash
# Check slow queries
sudo mysqldumpslow /var/log/mysql/slow.log

# Monitor connections
mysql -u root -p -e "SHOW PROCESSLIST;"

# Check error log
sudo tail -f /var/log/mysql/error.log
```

#### Tomcat
```bash
# Monitor logs
sudo tail -f /opt/tomcat/logs/catalina.out
sudo tail -f /opt/tomcat/logs/localhost.*.log

# Check thread dumps
sudo -u tomcat jstack $(pgrep -f catalina) > thread_dump.txt

# Monitor memory
sudo -u tomcat jstat -gc $(pgrep -f catalina) 1000
```

### Applying Updates & Security Patches

#### MySQL
```bash
sudo apt update
sudo apt list --upgradable | grep mysql
sudo apt upgrade mysql-server
sudo systemctl restart mysql
```

#### Tomcat
- Download latest patch release from [Apache Tomcat](https://tomcat.apache.org/)
- Backup current installation:
  ```bash
  sudo cp -r /opt/tomcat /opt/tomcat.bak
  ```
- Extract new version and replace binaries (preserve `conf/`, `webapps/`, `logs/`)
- Restart:
  ```bash
  sudo systemctl restart tomcat
  ```

### Log Analysis & Error Resolution

#### MySQL Logs
- **Error log:** `/var/log/mysql/error.log`
- **Slow query log:** `/var/log/mysql/slow.log`
- **Binary log:** `/var/log/mysql/mysql-bin.*`

#### Tomcat Logs
- **Catalina log:** `/opt/tomcat/logs/catalina.out`
- **Localhost log:** `/opt/tomcat/logs/localhost.YYYY-MM-DD.log`
- **Access log:** `/opt/tomcat/logs/localhost_access_log.YYYY-MM-DD.txt`

### Backup Strategy

#### MySQL Backups
```bash
# Daily backup script
sudo vim /usr/local/bin/mysql-backup.sh
```

```bash
#!/bin/bash
BACKUP_DIR="/var/backups/mysql"
DATE=$(date +%Y%m%d_%H%M%S)
mkdir -p $BACKUP_DIR
mysqldump -u root -p'ROOT_PASSWORD' --all-databases | gzip > $BACKUP_DIR/all-databases-$DATE.sql.gz
find $BACKUP_DIR -name "*.sql.gz" -mtime +7 -delete
```

Make executable and add to cron:
```bash
sudo chmod +x /usr/local/bin/mysql-backup.sh
sudo crontab -e
# Add:
0 2 * * * /usr/local/bin/mysql-backup.sh
```

#### Tomcat Backups
```bash
# Backup webapps and config
sudo tar -czf /var/backups/tomcat-$(date +%Y%m%d).tar.gz /opt/tomcat/webapps /opt/tomcat/conf
```

---

## Troubleshooting

### MySQL Won't Start
```bash
# Check error log
sudo tail -100 /var/log/mysql/error.log

# Check disk space
df -h

# Check permissions
ls -la /var/lib/mysql

# Try safe mode
sudo mysqld_safe --skip-grant-tables &
```

### Tomcat Won't Start
```bash
# Check catalina.out
sudo tail -100 /opt/tomcat/logs/catalina.out

# Check Java version
java -version

# Check ports
sudo netstat -tulpn | grep 8080

# Check permissions
ls -la /opt/tomcat

# Test startup manually
sudo -u tomcat /opt/tomcat/bin/catalina.sh run
```

### Connection Refused
- Check firewall: `sudo ufw status`
- Check service status: `sudo systemctl status mysql tomcat`
- Check listening ports: `sudo netstat -tulpn`

### To Reduce High Memory Usage
- MySQL: tuned `innodb_buffer_pool_size`
- Tomcat: adjusted JVM heap (`-Xmx`)
- Checked for memory leaks: analyze heap dumps

---

## Handover Documentation

### Credentials & Access
- **MySQL root password:** `[PROVIDED_SEPARATELY]`
- **MySQL appuser password:** `[PROVIDED_SEPARATELY]`
- **Tomcat admin password:** `[PROVIDED_SEPARATELY]`
- **Keystore password:** `[PROVIDED_SEPARATELY]`

### Service Endpoints
- **MySQL:** `localhost:3306` (internal only)
- **Tomcat HTTP:** `http://SERVER_IP:8080`
- **Tomcat HTTPS:** `https://SERVER_IP:8443`
- **Tomcat Manager:** `http://SERVER_IP:8080/manager/html`

### Important Files & Locations
- **MySQL config:** `/etc/mysql/mysql.conf.d/mysqld.cnf`
- **MySQL data:** `/var/lib/mysql`
- **Tomcat home:** `/opt/tomcat`
- **Tomcat config:** `/opt/tomcat/conf/server.xml`, `/opt/tomcat/conf/tomcat-users.xml`
- **Tomcat logs:** `/opt/tomcat/logs/`
- **Systemd service:** `/etc/systemd/system/tomcat.service`

### Useful Commands
```bash
# Restart services
sudo systemctl restart mysql
sudo systemctl restart tomcat

# Check logs
sudo tail -f /var/log/mysql/error.log
sudo tail -f /opt/tomcat/logs/catalina.out

# Check status
sudo systemctl status mysql tomcat
```



