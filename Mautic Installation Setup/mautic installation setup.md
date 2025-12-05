# Mautic Installation & Setup on DigitalOcean


This document records the completed freelance project to install and secure Mautic on a DigitalOcean Droplet and expose it under a subdomain.

## Original job description (client posted)


I need someone with extensive experience who knows how to install Mautic on a server using my DigitalOcean account, with everything configured under a subdomain.
Additionally, it must be set up with maximum security. This project has a fixed budget and must be completed immediately.
Therefore, if you have never done this before, do not bid on this job.

Deliverables -
Install and setup Mautic on DigitalOcean server,
Configure Mautic under a subdomain,
Ensure maximum security setup,
Provide documentation for setup

## Environment

- Provider: DigitalOcean (Droplet)
- OS: Ubuntu 24.04 LTS (server)
- Web server: Nginx
- PHP: 8.1
- DB: MariaDB
- SSL: Let's Encrypt (certbot)
- Mautic path: `/var/www/mautic`


## Files and locations created/edited

- Mautic web root: `/var/www/mautic`
- Nginx site: `/etc/nginx/sites-available/<SUBDOMAIN>` (symlinked to `/etc/nginx/sites-enabled/`)
- Cron file: `/etc/cron.d/mautic`
- Backup scripts: `/usr/local/bin/mautic-backup.sh`
- DB backups directory: `/var/backups/mautic/`
- Certbot/Let's Encrypt files: `/etc/letsencrypt/`

## High-level actions performed

1. Created Droplet with Ubuntu and SSH key.
2. Created non-root admin user and disabled root password login.
3. Installed updates and required packages (nginx, php-fpm, mariadb, certbot, ufw, fail2ban, composer, git).
4. Created MariaDB database and least-privilege user for Mautic.
5. Deployed Mautic application into `/var/www/mautic`
6. Configured Nginx site for the subdomain and applied recommended security headers.
7. Obtained TLS certificate via Certbot and enabled auto-renewal.
8. Hardened OS: UFW firewall, fail2ban, SSH hardening, file permissions, mysql_secure_installation.
9. Configured cron jobs required by Mautic and verified cron execution as `www-data`.
10. Implemented simple backup (DB dump + web root archive) and configured retention.

## Important commands

Note: run commands as appropriate user (root or with sudo). Replace placeholders before running.

### System prep
```bash
apt update && apt upgrade -y
apt install -y nginx mariadb-server certbot python3-certbot-nginx ufw fail2ban git unzip curl composer
```

### Create admin user and harden SSH (run as root)
```bash
adduser deploy
usermod -aG sudo deploy
sed -i 's/^PermitRootLogin yes/PermitRootLogin prohibit-password/' /etc/ssh/sshd_config
sed -i 's/^#PasswordAuthentication yes/PasswordAuthentication no/' /etc/ssh/sshd_config
systemctl reload sshd
```

### MariaDB secure setup and DB creation
```bash
mysql_secure_installation
mysql -u root -p <<SQL
CREATE DATABASE mautic CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'mauticuser'@'localhost' IDENTIFIED BY '<DB_PASSWORD>';
GRANT ALL PRIVILEGES ON mautic.* TO 'mauticuser'@'localhost';
FLUSH PRIVILEGES;
SQL
```

### PHP & extensions
```bash
apt install -y php8.1-fpm php8.1-cli php8.1-mbstring php8.1-xml php8.1-mysql php8.1-curl php8.1-intl php8.1-zip php8.1-gd
```

### Deploy Mautic
```bash
cd /var/www
git clone https://github.com/mautic/mautic.git mautic
cd mautic
composer install --no-dev --optimize-autoloader
chown -R www-data:www-data /var/www/mautic
chmod -R 750 /var/www/mautic
```

### Nginx site
Created `/etc/nginx/sites-available/<SUBDOMAIN>` with contents similar to:

```nginx
server {
    listen 80;
    server_name <SUBDOMAIN.example.com>;
    root /var/www/mautic;
    index index.php index.html;

    add_header X-Frame-Options "SAMEORIGIN";
    add_header X-Content-Type-Options "nosniff";
    add_header Referrer-Policy "no-referrer-when-downgrade";

    location / {
        try_files $uri $uri/ /index.php?$args;
    }

    location ~ \.php$ {
        include snippets/fastcgi-php.conf;
        fastcgi_pass unix:/run/php/php8.1-fpm.sock;
    }

    location ~ /\. {
        deny all;
    }
}
```

Enable site and reload nginx:
```bash
ln -s /etc/nginx/sites-available/<SUBDOMAIN> /etc/nginx/sites-enabled/
nginx -t && systemctl reload nginx
```

### Obtain TLS certificate
```bash
certbot --nginx -d <SUBDOMAIN.example.com> --non-interactive --agree-tos -m admin@<XXXX.com>
```

### Cron jobs for Mautic
```
* * * * * www-data php /var/www/mautic/bin/console mautic:segments:update >/dev/null 2>&1
* * * * * www-data php /var/www/mautic/bin/console mautic:campaigns:update >/dev/null 2>&1
* * * * * www-data php /var/www/mautic/bin/console mautic:campaigns:trigger >/dev/null 2>&1
*/5 * * * * www-data php /var/www/mautic/bin/console mautic:emails:send >/dev/null 2>&1
```

## Security hardening applied

- SSH: root login restricted, password auth disabled, key-based auth enforced.
- UFW: only ports 22, 80, 443 allowed.
- fail2ban: enabled with basic jails for ssh and nginx.
- MariaDB: `mysql_secure_installation` run and least-privilege DB user created.
- File permissions: web root owned by `www-data` with 750 where appropriate.
- TLS: Let's Encrypt certificates with auto-renew (Certbot systemd timers).
- HTTP security headers

## Deliverables delivered to clients

- SSH key access to `deploy` (non-root) user.
- Nginx site configuration file (path above).
- Cron `/etc/cron.d/mautic` file.
- This documentation file.


---
