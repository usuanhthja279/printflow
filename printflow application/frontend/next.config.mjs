/** @type {import('next').NextConfig} */
const nextConfig={
  output:'standalone',
  // Allows a verification build alongside a running local standalone server.
  distDir:process.env.PRINTFLOW_NEXT_DIST_DIR||'.next',
  // The HTTP rewrite below does not proxy WebSocket upgrades. Local collaboration
  // connects directly to the authenticated Spring endpoint; production must route
  // /collaboration/ws through its same-origin reverse proxy.
  experimental:{cpus:2,proxyTimeout:90000,proxyClientMaxBodySize:"22mb"},
  async rewrites(){
    const base = process.env.API_INTERNAL_URL || 'http://localhost:8080';
    return [{source:'/api/:path*',destination:`${base}/api/:path*`}];
  },
};
export default nextConfig;
