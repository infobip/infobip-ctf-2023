WORKDIR="/tmp/cloud"

mkdir -p "${WORKDIR}"
cp f "${WORKDIR}"
cd "${WORKDIR}" && fossil init mycloud --project-name mycloud -A vm
cd "${WORKDIR}" && fossil user password vm pass -R mycloud
cd "${WORKDIR}" && fossil user default vm
cd "${WORKDIR}" && fossil open -f mycloud
cd "${WORKDIR}" && fossil add f
cd "${WORKDIR}" && fossil commit --no-warnings -m 'backup'
cd "${WORKDIR}" && fossil server
cd "${WORKDIR}" && fossil close
