# Deploying HelloApp to AWS Free Tier (EC2 + Docker)

This guide deploys the Spring Boot **HelloApp** to a single **EC2 t3.micro** instance
(Free Tier eligible) running the app as a Docker container.

The image is built **locally** and shipped to the instance, so the tiny 1 GB t2.micro
never has to run a heavy Maven build.

```
Local machine                         AWS (us-east-1)
+-----------------+   scp image tar   +---------------------------+
| docker build    | ----------------> | EC2 t2.micro (AL2023)     |
| docker save     |                   |  docker load + docker run |
| provision/deploy|   aws cli / ssh   |  port 80 -> container 8080|
+-----------------+ ----------------> +---------------------------+
```

## Prerequisites

| Tool            | Check                     | Status on your machine |
|-----------------|---------------------------|------------------------|
| AWS CLI v2      | `aws --version`           | ✅ installed           |
| Docker Desktop  | `docker --version`        | ✅ installed (must be **running**) |
| OpenSSH client  | `ssh -V`                  | Built into Windows 10/11 |
| AWS account     | Free Tier, billing set up | ✅ ready               |

## Step 1 — Configure AWS credentials (one time)

Create an IAM user (or use IAM Identity Center) with programmatic access and the
`AmazonEC2FullAccess` policy, then run:

```powershell
aws configure
```

Enter your **Access Key ID**, **Secret Access Key**, default region (e.g. `us-east-1`),
and output format `json`. Verify:

```powershell
aws sts get-caller-identity
```

> ⚠️ Never commit your access keys. `aws configure` stores them in `%USERPROFILE%\.aws\`.

## Step 2 — Provision the EC2 instance

```powershell
cd deploy\aws
.\provision-ec2.ps1
```

This creates a key pair (`helloapp-key.pem`), a security group (SSH from **your IP**
only, HTTP 80 open to the world), and launches the instance. Docker + a 2 GB swap file
are installed automatically via user-data. Details are saved to `instance-state.json`.

Optional parameters: `-Region`, `-InstanceType`, `-KeyName`, `-SgName`, `-InstanceName`.

## Step 3 — Build and deploy the app

```powershell
.\deploy-app.ps1
```

This builds the Docker image locally, copies it to the instance, starts the container
(`SPRING_PROFILES_ACTIVE=prod`, port 80 → 8080, persistent volumes for the H2 DB and
logs), and verifies the endpoint. On success it prints your public URL.

- App:       `http://<public-ip>/`
- Employees: `http://<public-ip>/employees` — login **partner / partner123**

To redeploy after code changes, just run `.\deploy-app.ps1` again.

## Step 4 — Tear down (stop all charges)

```powershell
.\teardown-ec2.ps1
```

Terminates the instance and deletes the security group and key pair. Run this whenever
you're done to avoid consuming Free-Tier hours.

## Production profile

Deployment uses `src/main/resources/application-prod.properties`:

- Port **8080** (mapped to host **80**)
- H2 **file** database persisted in the `/app/data` Docker volume (survives restarts/redeploys)
- **H2 console disabled** (never expose a DB console on the public internet)
- INFO-level logging under `/app/logs`

## Security notes

- SSH (port 22) is restricted to the public IP detected when you provisioned. If your IP
  changes, add a new rule or re-run provisioning.
- The app still uses the basic credentials `partner / partner123` from
  `application.properties`. **Change these before any real-world use.**
- HTTP only (no TLS). For a real deployment, put the instance behind a load balancer or
  add a reverse proxy (nginx/Caddy) with a certificate.

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| `AWS credentials not configured` | Run `aws configure` (Step 1). |
| `docker build failed` | Ensure Docker Desktop is running. |
| scp/ssh `Permission denied` | The script locks down the `.pem`; ensure you're the file owner. |
| HTTP check not 200 | Wait ~30s; check `ssh -i helloapp-key.pem ec2-user@<ip> 'docker logs helloapp'`. |
| Instance "not ready" loop | user-data (Docker install) can take 1-2 min after boot; the script retries for 5 min. |
