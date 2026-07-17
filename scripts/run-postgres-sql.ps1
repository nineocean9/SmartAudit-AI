param(
  [Parameter(Mandatory = $true, Position = 0)]
  [string]$File,

  [string]$HostName = "127.0.0.1",
  [int]$Port = 5432,
  [string]$Database = "ry-vue",
  [string]$User = "postgres",
  [string]$Password = "123456"
)

$ErrorActionPreference = "Stop"

$pgBin = "D:\Program Files\PostgreSQL\18\bin"
if (Test-Path -LiteralPath $pgBin) {
  $env:Path = "$pgBin;$env:Path"
}

$psql = Get-Command psql -ErrorAction Stop
$sqlPath = Resolve-Path -LiteralPath $File
$env:PGPASSWORD = $Password

try {
  & $psql.Source -h $HostName -p $Port -U $User -d $Database -v ON_ERROR_STOP=1 -f $sqlPath
} finally {
  Remove-Item Env:\PGPASSWORD -ErrorAction SilentlyContinue
}
