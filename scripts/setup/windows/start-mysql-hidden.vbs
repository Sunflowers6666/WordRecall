' Hidden MySQL launcher for the Windows Startup folder (optional).
' Place a shortcut to this file in shell:startup if you want MySQL to start at logon.
Set sh = CreateObject("WScript.Shell")
sh.Run """E:\Develop\MySQL\mysql-8.0.46-winx64\bin\mysqld.exe"" --defaults-file=E:\Develop\MySQL\my.ini", 0, False
