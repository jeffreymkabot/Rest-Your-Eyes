;Author: Sean Pesce
;This file will be executed next to the application bundle image
;I.e. current directory will contain folder RestYourEyes with application files
#define VerFile FileOpen("..\..\rsrc\version")
#define AppVer FileRead(VerFile)
#expr FileClose(VerFile)
#undef VerFile
[Setup]
AppId=restyoureyes
AppName=RestYourEyes Reminder
AppVersion=1.0
AppVerName=RestYourEyes Reminder v{#AppVer}
AppPublisher=Sean Pesce
AppComments=Reminds the user to take a break from their screen to avoid eye strain
AppCopyright=Copyright (C) 2017
AppPublisherURL=https://twitter.com/SeanPesce
AppSupportURL=https://reddit.com/u/SeanPesce
AppUpdatesURL=https://github.com/SeanPesce
DefaultDirName={pf}\SeanP Software\RestYourEyes
DisableStartupPrompt=Yes
DisableDirPage=No
DisableProgramGroupPage=Yes
DisableReadyPage=Yes
DisableFinishedPage=No
DisableWelcomePage=Yes
DefaultGroupName=SeanP Software
;Optional License:
;LicenseFile=
;WinXP or above:
MinVersion=0,5.1 
OutputBaseFilename=RestYourEyes-{#AppVer}_setup
Compression=lzma
SolidCompression=yes
PrivilegesRequired=lowest
SetupIconFile=RestYourEyes\RestYourEyes.ico
UninstallDisplayIcon={app}\RestYourEyes.exe
UninstallDisplayName=RestYourEyes Reminder
WizardImageStretch=No
WizardSmallImageFile=..\..\rsrc\icons\package\windows\RestYourEyes-setup-icon.bmp
;WizardImageBackColor=
;WizardImageFile=
ArchitecturesInstallIn64BitMode=x64
VersionInfoVersion={#AppVer}.0.0
OutputDir=Installer
;BackColor=$2A446D
;BackColor2=clBlack



[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Files]
Source: "RestYourEyes\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\RestYourEyes Reminder"; Filename: "{app}\RestYourEyes.exe"; IconFilename: "{app}\RestYourEyes.ico"; Check: returnTrue()


[Run]
Filename: "{app}\RestYourEyes.exe"; Parameters: "-Xappcds:generatecache"; Check: returnFalse()
Filename: "{app}\RestYourEyes.exe"; Description: "{cm:LaunchProgram,RestYourEyes}"; Flags: nowait postinstall skipifsilent; Check: returnTrue()
Filename: "{app}\RestYourEyes.exe"; Parameters: "-install -svcName ""RestYourEyes"" -svcDesc ""RestYourEyes"" -mainExe ""RestYourEyes.exe""  "; Check: returnFalse()

[UninstallDelete]
;Type: files; Name: "{app}\path_to\extra_files\to_delete_when\uninstalling\*.png"
;Type: dirifempty; Name: "{app}\path_to\extra_files\to_delete_when\uninstalling"
;Type: dirifempty; Name: "{app}\path_to\extra_files\to_delete_when"
;Type: dirifempty; Name: "{app}\path_to\extra_files"
;Type: dirifempty; Name: "{app}\path_to"
;Type: dirifempty; Name: "{app}"

[UninstallRun]
Filename: "{app}\RestYourEyes.exe "; Parameters: "-uninstall -svcName RestYourEyes -stopOnUninstall"; Check: returnFalse()

[Tasks]
;Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; (creates desktop icon or start menu icon?)

[CustomMessages]
AppName=RestYourEyes
LaunchProgram=Start RestYourEyes after finishing installation

[Code]
function returnTrue(): Boolean;
begin
  Result := True;
end;

function returnFalse(): Boolean;
begin
  Result := False;
end;

function InitializeSetup(): Boolean;
begin
// Possible future improvements:
//   if version less or same => just launch app
//   if upgrade => check if same app is running and wait for it to exit
//   Add pack200/unpack200 support? 
  Result := True;
end;  