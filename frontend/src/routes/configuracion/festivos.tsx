import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/configuracion/festivos')({
  component: RouteComponent,
})

function RouteComponent() {
  return <div>Hello "/configuracion/festivos"!</div>
}
