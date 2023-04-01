CURR_LOC=$(pwd)
WORKDIR="/tmp/work"

mkdir -p "${WORKDIR}"
cd "${WORKDIR}" && fossil clone --save-http-password http://vm:pass@127.0.0.1:8080/mycloud
cd "${WORKDIR}" && cp "${CURR_LOC}/La mejor canción" "${CURR_LOC}/.seguridad.kdbx" .
cd "${WORKDIR}" && fossil open -f mycloud.fossil
cd "${WORKDIR}" && fossil add La\ mejor\ canción .seguridad.kdbx
cd "${WORKDIR}" && fossil commit --no-warnings -m 'forgot these'
cd "${WORKDIR}" && fossil push -B vm:pass http://127.0.0.1:8080/mycloud
cd "${WORKDIR}" && fossil close
rm -rf "${WORKDIR}"
