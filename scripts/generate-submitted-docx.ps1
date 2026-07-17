param(
    [string]$PsqlPath = "D:\Program Files\PostgreSQL\18\bin\psql.exe",
    [string]$Database = "ry-vue",
    [string]$User = "postgres",
    [string]$Password = "123456",
    [string]$ProfileRoot = "D:\ruoyi\uploadPath",
    [switch]$Overwrite
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath $PsqlPath)) {
    throw "psql not found: $PsqlPath"
}

Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

function Escape-Xml([string]$Text) {
    if ($null -eq $Text) { return "" }
    return [System.Security.SecurityElement]::Escape($Text)
}

function New-Paragraph([string]$Text, [string]$Style = "Normal") {
    $escaped = Escape-Xml $Text
    return "<w:p><w:pPr><w:pStyle w:val=""$Style""/></w:pPr><w:r><w:t xml:space=""preserve"">$escaped</w:t></w:r></w:p>"
}

function Add-ZipEntry([System.IO.Compression.ZipArchive]$Zip, [string]$Name, [string]$Content) {
    $entry = $Zip.CreateEntry($Name)
    $writer = New-Object System.IO.StreamWriter($entry.Open(), [System.Text.UTF8Encoding]::new($false))
    try {
        $writer.Write($Content)
    } finally {
        $writer.Dispose()
    }
}

function New-Docx([string]$OutputPath, [object]$Row) {
    $title = "$($Row.project_name) - $($Row.material_name)"
    $parts = New-Object System.Collections.Generic.List[string]
    $parts.Add((New-Paragraph -Text $title -Style "Title"))
    $parts.Add((New-Paragraph -Text "Material summary" -Style "Heading1"))
    $parts.Add((New-Paragraph -Text "Project: $($Row.project_name)"))
    $parts.Add((New-Paragraph -Text "Audited unit: $($Row.audited_unit)"))
    $parts.Add((New-Paragraph -Text "Material: $($Row.material_name)"))
    $parts.Add((New-Paragraph -Text "Category: $($Row.material_type)"))
    $parts.Add((New-Paragraph -Text "Submitted by: $($Row.submit_by)"))
    $parts.Add((New-Paragraph -Text "Submitted at: $($Row.submit_time)"))
    $parts.Add((New-Paragraph -Text "Generated content" -Style "Heading1"))
    $parts.Add((New-Paragraph -Text "This Word document was generated from the audit preparation material checklist because the database row had a submitted file path but the physical DOCX file was missing."))
    $parts.Add((New-Paragraph -Text "It corresponds to material '$($Row.material_name)' in project '$($Row.project_name)' and keeps the demo workflow usable for preview, download, and replacement upload."))
    $parts.Add((New-Paragraph -Text "Review notes" -Style "Heading1"))
    $parts.Add((New-Paragraph -Text "1. Confirm the material name, project, submitter, and submit time against the system checklist."))
    $parts.Add((New-Paragraph -Text "2. In a production workflow, replace this generated demo file with the real source document or scanned evidence."))
    $parts.Add((New-Paragraph -Text "3. Re-uploading a real document from the audit preparation page will replace this file path."))
    $paragraphs = [string]::Join("`n", $parts)

    $documentXml = @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:body>
    $paragraphs
    <w:sectPr>
      <w:pgSz w:w="11906" w:h="16838"/>
      <w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440" w:header="720" w:footer="720" w:gutter="0"/>
    </w:sectPr>
  </w:body>
</w:document>
"@

    $stylesXml = @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:docDefaults>
    <w:rPrDefault>
      <w:rPr>
        <w:rFonts w:ascii="Calibri" w:hAnsi="Calibri" w:eastAsia="SimSun" w:cs="Arial"/>
        <w:sz w:val="22"/>
        <w:szCs w:val="22"/>
        <w:lang w:val="en-US" w:eastAsia="zh-CN"/>
      </w:rPr>
    </w:rPrDefault>
    <w:pPrDefault>
      <w:pPr>
        <w:spacing w:after="160" w:line="276" w:lineRule="auto"/>
      </w:pPr>
    </w:pPrDefault>
  </w:docDefaults>
  <w:style w:type="paragraph" w:default="1" w:styleId="Normal">
    <w:name w:val="Normal"/>
    <w:qFormat/>
    <w:pPr>
      <w:spacing w:after="160" w:line="276" w:lineRule="auto"/>
    </w:pPr>
    <w:rPr>
      <w:rFonts w:ascii="Calibri" w:hAnsi="Calibri" w:eastAsia="SimSun"/>
      <w:sz w:val="22"/>
    </w:rPr>
  </w:style>
  <w:style w:type="paragraph" w:styleId="Title">
    <w:name w:val="Title"/>
    <w:basedOn w:val="Normal"/>
    <w:next w:val="Normal"/>
    <w:qFormat/>
    <w:pPr>
      <w:jc w:val="center"/>
      <w:spacing w:after="320"/>
    </w:pPr>
    <w:rPr>
      <w:rFonts w:ascii="Calibri" w:hAnsi="Calibri" w:eastAsia="Microsoft YaHei"/>
      <w:b/>
      <w:sz w:val="36"/>
      <w:color w:val="1F3864"/>
    </w:rPr>
  </w:style>
  <w:style w:type="paragraph" w:styleId="Heading1">
    <w:name w:val="heading 1"/>
    <w:basedOn w:val="Normal"/>
    <w:next w:val="Normal"/>
    <w:qFormat/>
    <w:pPr>
      <w:keepNext/>
      <w:spacing w:before="360" w:after="120"/>
      <w:outlineLvl w:val="0"/>
    </w:pPr>
    <w:rPr>
      <w:rFonts w:ascii="Calibri" w:hAnsi="Calibri" w:eastAsia="Microsoft YaHei"/>
      <w:b/>
      <w:sz w:val="28"/>
      <w:color w:val="1F3864"/>
    </w:rPr>
  </w:style>
</w:styles>
"@

    $contentTypes = @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
  <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
  <Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
  <Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
</Types>
"@

    $rels = @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
</Relationships>
"@

    $docRels = @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>
"@

    $created = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
    $core = @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:dcmitype="http://purl.org/dc/dcmitype/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <dc:title>$(Escape-Xml $title)</dc:title>
  <dc:creator>RuoYi Audit Demo</dc:creator>
  <cp:lastModifiedBy>RuoYi Audit Demo</cp:lastModifiedBy>
  <dcterms:created xsi:type="dcterms:W3CDTF">$created</dcterms:created>
  <dcterms:modified xsi:type="dcterms:W3CDTF">$created</dcterms:modified>
</cp:coreProperties>
"@

    $app = @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties" xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes">
  <Application>RuoYi Audit Demo</Application>
</Properties>
"@

    $dir = Split-Path -Parent $OutputPath
    if (-not (Test-Path -LiteralPath $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
    }
    if (Test-Path -LiteralPath $OutputPath) {
        Remove-Item -LiteralPath $OutputPath -Force
    }

    $zip = [System.IO.Compression.ZipFile]::Open($OutputPath, [System.IO.Compression.ZipArchiveMode]::Create)
    try {
        Add-ZipEntry $zip "[Content_Types].xml" $contentTypes
        Add-ZipEntry $zip "_rels/.rels" $rels
        Add-ZipEntry $zip "word/document.xml" $documentXml
        Add-ZipEntry $zip "word/styles.xml" $stylesXml
        Add-ZipEntry $zip "word/_rels/document.xml.rels" $docRels
        Add-ZipEntry $zip "docProps/core.xml" $core
        Add-ZipEntry $zip "docProps/app.xml" $app
    } finally {
        $zip.Dispose()
    }
}

$query = @"
SELECT c.id,
       c.project_id,
       COALESCE(p.project_name, '审计项目' || c.project_id) AS project_name,
       COALESCE(p.audited_unit, '') AS audited_unit,
       c.material_name,
       COALESCE(c.material_type, '') AS material_type,
       COALESCE(c.submit_by, '') AS submit_by,
       COALESCE(to_char(c.submit_time, 'YYYY-MM-DD HH24:MI:SS'), '') AS submit_time,
       c.file_path
FROM audit_material_checklist c
LEFT JOIN audit_project p ON p.id = c.project_id
WHERE c.file_path IS NOT NULL
  AND lower(c.file_path) LIKE '%.docx'
ORDER BY c.project_id, c.id;
"@

$env:PGPASSWORD = $Password
$rows = & $PsqlPath -U $User -d $Database --csv -c $query | ConvertFrom-Csv

$createdCount = 0
$skippedCount = 0
foreach ($row in $rows) {
    if (-not $row.file_path.StartsWith("/profile/")) {
        Write-Warning "Skip non-profile path: $($row.file_path)"
        continue
    }
    $relative = $row.file_path.Substring("/profile/".Length).Replace("/", [System.IO.Path]::DirectorySeparatorChar)
    $target = Join-Path $ProfileRoot $relative
    if ((Test-Path -LiteralPath $target) -and -not $Overwrite) {
        $skippedCount++
        continue
    }
    New-Docx -OutputPath $target -Row $row
    $createdCount++
    Write-Host "generated $target"
}

Write-Host "created=$createdCount skipped=$skippedCount total=$($rows.Count)"
