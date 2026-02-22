import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/calendario/test')({
  component: RouteComponent,
})

function RouteComponent() {
  return <div>Hello "/calendario/test"!</div>
}
